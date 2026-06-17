from pathlib import Path
from typing import Dict, List, Optional, Any

import joblib
import numpy as np
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel, Field, ConfigDict

BASE_DIR = Path(__file__).resolve().parent
MODEL_DIR = BASE_DIR / "models"

FEATURE_ORDER = ["temperature", "ph", "ec_value", "salinity", "do_value"]

# QCVN 02-19:2014/BNNPTNT - default thresholds
TEMP_MIN = 18.0
TEMP_MAX = 33.0
PH_MIN = 7.0
PH_MAX = 9.0
SALINITY_MIN = 5.0
SALINITY_MAX = 35.0
DO_MIN = 3.5


def safe_load(path: Path):
    if path.exists():
        return joblib.load(path)
    return None


# Old AI layer
iso_scaler = safe_load(MODEL_DIR / "isolation_forest_scaler.joblib")
iso_model = safe_load(MODEL_DIR / "isolation_forest_model.joblib")
xgb_scaler = safe_load(MODEL_DIR / "xgboost_scaler.joblib")
xgb_model = safe_load(MODEL_DIR / "xgboost_status_model.joblib")
label_encoder = safe_load(MODEL_DIR / "status_label_encoder.joblib")

# New Random Forest layer
rf_artifact = safe_load(MODEL_DIR / "shrimp_ai_model.joblib")
rf_model = None
rf_features = FEATURE_ORDER
rf_classes = None

if isinstance(rf_artifact, dict):
    rf_model = rf_artifact.get("model")
    rf_features = rf_artifact.get("features", FEATURE_ORDER)
    rf_classes = rf_artifact.get("classes")
else:
    rf_model = rf_artifact


app = FastAPI(
    title="Shrimp IoT AI Service",
    version="2.0.0",
    description="AI service combining rule-based QCVN, Isolation Forest, XGBoost, and Random Forest.",
)


class PredictRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    temperature: float
    ph: float
    ec_value: float = Field(alias="ec_value")
    salinity: float
    do_value: float = Field(alias="do_value")


def normalize_status(value: Any) -> str:
    if isinstance(value, np.ndarray):
        value = value[0]

    if isinstance(value, (np.integer, int, float, np.floating)):
        numeric = int(value)
        mapping = {
            0: "NORMAL",
            1: "WARNING",
            2: "DANGER",
        }
        return mapping.get(numeric, "WARNING")

    status = str(value).strip().upper()
    if status in {"NORMAL", "WARNING", "DANGER"}:
        return status
    if status in {"ANOMALY", "ABNORMAL"}:
        return "WARNING"
    return "WARNING"


def status_rank(status: str) -> int:
    status = str(status).upper()
    if status == "DANGER":
        return 2
    if status == "WARNING" or status == "ANOMALY":
        return 1
    return 0


def max_status(*statuses: str) -> str:
    max_rank = max(status_rank(status) for status in statuses)
    if max_rank >= 2:
        return "DANGER"
    if max_rank == 1:
        return "WARNING"
    return "NORMAL"


def rule_based_check(payload: PredictRequest) -> tuple[str, str]:
    warnings: List[str] = []
    has_danger = False

    if payload.temperature < TEMP_MIN:
        warnings.append(f"Nhiệt độ thấp hơn ngưỡng QCVN ({TEMP_MIN}-{TEMP_MAX} °C)")
    elif payload.temperature > TEMP_MAX:
        warnings.append(f"Nhiệt độ cao hơn ngưỡng QCVN ({TEMP_MIN}-{TEMP_MAX} °C)")

    if payload.ph < PH_MIN:
        warnings.append(f"pH thấp hơn ngưỡng QCVN ({PH_MIN}-{PH_MAX})")
    elif payload.ph > PH_MAX:
        warnings.append(f"pH cao hơn ngưỡng QCVN ({PH_MIN}-{PH_MAX})")

    if payload.salinity < SALINITY_MIN:
        warnings.append(f"Độ mặn thấp hơn ngưỡng QCVN ({SALINITY_MIN}-{SALINITY_MAX}‰)")
    elif payload.salinity > SALINITY_MAX:
        warnings.append(f"Độ mặn cao hơn ngưỡng QCVN ({SALINITY_MIN}-{SALINITY_MAX}‰)")

    if payload.do_value < DO_MIN:
        warnings.append(f"Oxy hòa tan thấp hơn ngưỡng QCVN (DO >= {DO_MIN} mg/L)")
        has_danger = True

    if not warnings:
        return "NORMAL", "Thông số môi trường đạt ngưỡng QCVN 02-19:2014/BNNPTNT"
    if has_danger or len(warnings) >= 2:
        return "DANGER", "; ".join(warnings)
    return "WARNING", "; ".join(warnings)


def predict_isolation_forest(x: np.ndarray) -> str:
    if iso_model is None or iso_scaler is None:
        return "NOT_RUN"
    iso_x = iso_scaler.transform(x)
    iso_raw = int(iso_model.predict(iso_x)[0])
    return "NORMAL" if iso_raw == 1 else "ANOMALY"


def predict_xgboost(x: np.ndarray) -> tuple[str, Optional[Dict[str, float]]]:
    if xgb_model is None or xgb_scaler is None or label_encoder is None:
        return "NOT_RUN", None

    xgb_x = xgb_scaler.transform(x)
    xgb_raw = xgb_model.predict(xgb_x)
    xgb_status = str(label_encoder.inverse_transform(xgb_raw.astype(int))[0])

    probabilities = None
    if hasattr(xgb_model, "predict_proba"):
        proba = xgb_model.predict_proba(xgb_x)[0]
        probabilities = {
            str(label): float(round(prob, 6))
            for label, prob in zip(label_encoder.classes_, proba)
        }

    return normalize_status(xgb_status), probabilities


def predict_random_forest(payload: PredictRequest) -> tuple[str, Optional[Dict[str, float]]]:
    if rf_model is None:
        return "NOT_RUN", None

    values = {
        "temperature": payload.temperature,
        "ph": payload.ph,
        "ec_value": payload.ec_value,
        "salinity": payload.salinity,
        "do_value": payload.do_value,
    }

    # Random Forest model was trained with feature names, so use DataFrame.
    df = pd.DataFrame([{name: values[name] for name in rf_features}], columns=rf_features)
    raw = rf_model.predict(df)[0]
    rf_status = normalize_status(raw)

    probabilities = None
    if hasattr(rf_model, "predict_proba"):
        proba = rf_model.predict_proba(df)[0]
        classes = getattr(rf_model, "classes_", None)
        if classes is None:
            classes = rf_classes
        probabilities = {
            str(label): float(round(prob, 6))
            for label, prob in zip(classes, proba)
        }

    return rf_status, probabilities


def ai_ensemble_decision(iso_status: str, xgb_status: str, rf_status: str) -> str:
    # Isolation Forest does not output DANGER. If it detects anomaly, AI status is at least WARNING.
    if xgb_status == "DANGER" or rf_status == "DANGER":
        return "DANGER"
    if iso_status == "ANOMALY":
        return "WARNING"
    if xgb_status == "WARNING" or rf_status == "WARNING":
        return "WARNING"
    if xgb_status == "NOT_RUN" and rf_status == "NOT_RUN" and iso_status == "NOT_RUN":
        return "NOT_RUN"
    return "NORMAL"


def final_decision(rule_status: str, ai_status: str) -> str:
    if ai_status == "NOT_RUN":
        return rule_status
    return max_status(rule_status, ai_status)


def recommendation_for(status: str) -> str:
    if status == "NORMAL":
        return "Tiếp tục giám sát định kỳ"
    if status == "WARNING":
        return "Kiểm tra lại cảm biến, theo dõi xu hướng và chuẩn bị biện pháp xử lý nếu thông số tiếp tục xấu đi"
    return "Kiểm tra ao ngay; ưu tiên xử lý oxy hòa tan, pH, độ mặn hoặc nhiệt độ theo thông số vượt chuẩn"


@app.get("/")
def root():
    return {
        "service": "Shrimp IoT AI Service",
        "status": "running",
        "endpoint": "/predict",
        "version": "2.0.0",
    }


@app.get("/health")
def health():
    return {
        "status": "UP",
        "feature_order": FEATURE_ORDER,
        "models": {
            "isolationForest": iso_model is not None and iso_scaler is not None,
            "xgboost": xgb_model is not None and xgb_scaler is not None and label_encoder is not None,
            "randomForest": rf_model is not None,
        },
        "randomForestFeatures": rf_features,
    }


@app.post("/predict")
def predict(payload: PredictRequest):
    x = np.array([[
        payload.temperature,
        payload.ph,
        payload.ec_value,
        payload.salinity,
        payload.do_value,
    ]], dtype=float)

    rule_status, rule_message = rule_based_check(payload)

    isolation_forest_status = predict_isolation_forest(x)
    xgboost_status, xgboost_probabilities = predict_xgboost(x)
    random_forest_status, random_forest_probabilities = predict_random_forest(payload)

    ai_status = ai_ensemble_decision(
        isolation_forest_status,
        xgboost_status,
        random_forest_status,
    )
    final_status = final_decision(rule_status, ai_status)

    return {
        "success": True,
        "feature_order": FEATURE_ORDER,

        # Rule layer
        "rule_status": rule_status,
        "ruleStatus": rule_status,
        "rule_message": rule_message,
        "ruleMessage": rule_message,

        # AI model layers
        "isolation_forest_status": isolation_forest_status,
        "isolationForestStatus": isolation_forest_status,
        "anomalyStatus": isolation_forest_status,

        "xgboost_status": xgboost_status,
        "xgboostStatus": xgboost_status,
        "mlStatus": xgboost_status,

        "random_forest_status": random_forest_status,
        "randomForestStatus": random_forest_status,

        # AI ensemble output
        "ai_status": ai_status,
        "aiStatus": ai_status,

        # Final output: rule + AI
        "final_status": final_status,
        "finalStatus": final_status,

        "xgboost_probabilities": xgboost_probabilities,
        "xgboostProbabilities": xgboost_probabilities,
        "random_forest_probabilities": random_forest_probabilities,
        "randomForestProbabilities": random_forest_probabilities,

        "recommendation": recommendation_for(final_status),
        "message": f"AI ensemble predicted {ai_status}; final status is {final_status}",
    }
