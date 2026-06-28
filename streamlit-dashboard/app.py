import os
from datetime import datetime

import altair as alt
import pandas as pd
import requests
import streamlit as st


DEFAULT_API_BASE_URL = os.getenv("SHRIMP_API_BASE_URL", "http://127.0.0.1:8080/api")
DEFAULT_AI_BASE_URL = os.getenv("SHRIMP_AI_BASE_URL", "http://127.0.0.1:8001")
DEFAULT_DEVICE_ID = os.getenv("SHRIMP_DEVICE_ID", "device_01")
REQUEST_TIMEOUT = 8


st.set_page_config(
    page_title="IoT- Hệ thống giám sát môi trường ao nuôi thủy hải sản",
    page_icon="",
    layout="wide",
    initial_sidebar_state="expanded",
)


st.markdown(
    """
    <style>
      :root {
        --ink: #15201b;
        --muted: #63736d;
        --line: #d7e0dc;
        --panel: #ffffff;
        --surface: #f5f8f6;
        --ok: #087f5b;
        --warn: #b25f00;
        --bad: #b42318;
      }
      .stApp { background: var(--surface); color: var(--ink); }
      .block-container { padding-top: 1.2rem; padding-bottom: 2rem; }
      section[data-testid="stSidebar"] { background: #ffffff; border-right: 1px solid var(--line); }
      [data-testid="stMetric"] {
        background: var(--panel);
        border: 1px solid var(--line);
        border-radius: 8px;
        padding: 14px 16px;
      }
      [data-testid="stMetricLabel"] { color: var(--muted); font-weight: 650; }
      div[data-testid="stDataFrame"] { border: 1px solid var(--line); border-radius: 8px; }
      .app-title { margin: 0 0 0.1rem; font-size: 2rem; line-height: 1.15; }
      .app-subtitle { color: var(--muted); margin-bottom: 1rem; }
      .panel {
        background: var(--panel);
        border: 1px solid var(--line);
        border-radius: 8px;
        padding: 14px 16px;
        min-height: 92px;
      }
      .label { color: var(--muted); font-size: 0.8rem; font-weight: 700; text-transform: uppercase; }
      .value { font-size: 1.6rem; font-weight: 800; margin-top: 0.3rem; overflow-wrap: anywhere; }
      .small { color: var(--muted); font-size: 0.9rem; margin-top: 0.2rem; }
      .ok { color: var(--ok); }
      .warn { color: var(--warn); }
      .bad { color: var(--bad); }
    </style>
    """,
    unsafe_allow_html=True,
)


def init_state() -> None:
    st.session_state.setdefault("token", "")
    st.session_state.setdefault("username", "")
    st.session_state.setdefault("api_base_url", DEFAULT_API_BASE_URL)
    st.session_state.setdefault("ai_base_url", DEFAULT_AI_BASE_URL)
    st.session_state.setdefault("device_id", DEFAULT_DEVICE_ID)
    st.session_state.setdefault("chat_session_id", None)
    st.session_state.setdefault("chat_messages", [])


def request_json(method: str, url: str, token: str = "", json_body=None):
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    try:
        response = requests.request(
            method,
            url,
            headers=headers,
            json=json_body,
            timeout=REQUEST_TIMEOUT,
        )
        content_type = response.headers.get("content-type", "")
        payload = response.json() if "application/json" in content_type else {"raw": response.text}
        if response.status_code >= 400:
            return None, f"{response.status_code} {response.reason}"
        return payload, None
    except requests.RequestException as exc:
        return None, str(exc)


def unwrap(payload):
    if isinstance(payload, dict) and "data" in payload:
        return payload.get("data")
    return payload


def get_api(path: str, token: str = "", params: dict | None = None):
    base = st.session_state.api_base_url.rstrip("/")
    query = ""
    if params:
        query = "?" + requests.compat.urlencode(params)
    return request_json("GET", f"{base}{path}{query}", token=token)


def login(username: str, password: str):
    base = st.session_state.api_base_url.rstrip("/")
    payload, error = request_json(
        "POST",
        f"{base}/auth/login",
        json_body={"username": username, "password": password},
    )
    if error:
        return error
    data = unwrap(payload) or {}
    token = data.get("token", "")
    if not token:
        return "Đăng nhập không thành công."
    st.session_state.token = token
    st.session_state.username = (data.get("user") or {}).get("username", username)
    return ""


def status_class(value: str) -> str:
    value = (value or "").upper()
    if value in {"UP", "ONLINE", "NORMAL", "ACK", "RESOLVED", "OFF"}:
        return "ok"
    if value in {"WARNING", "SENT", "PENDING", "OPEN", "ON"}:
        return "warn"
    if value in {"DANGER", "ERROR", "OFFLINE", "FAILED", "EXPIRED"}:
        return "bad"
    return "ok"


def status_text(value: str) -> str:
    mapping = {
        "UP": "Đang hoạt động",
        "ONLINE": "Đang kết nối",
        "OFFLINE": "Mất kết nối",
        "NORMAL": "Ổn định",
        "WARNING": "Cần theo dõi",
        "DANGER": "Cần xử lý",
        "OPEN": "Chưa xử lý",
        "RESOLVED": "Đã xử lý",
        "ACK": "Đã xác nhận",
        "SENT": "Đã gửi",
        "PENDING": "Đang chờ",
        "FAILED": "Thất bại",
        "EXPIRED": "Hết hạn",
        "ON": "Đang bật",
        "OFF": "Đang tắt",
        "ACTIVE": "Đang hoạt động",
        "INACTIVE": "Ngừng hoạt động",
    }
    return mapping.get((value or "").upper(), value or "Không rõ")


def value_or_dash(value, suffix: str = "") -> str:
    if value is None or value == "":
        return "-"
    if isinstance(value, float):
        value = round(value, 2)
    return f"{value}{suffix}"


def card(label: str, value: str, note: str = "", state: str = "ok"):
    st.markdown(
        f"""
        <div class="panel">
          <div class="label">{label}</div>
          <div class="value {state}">{value}</div>
          <div class="small">{note}</div>
        </div>
        """,
        unsafe_allow_html=True,
    )


def friendly_table(data, rename: dict[str, str], columns: list[str]):
    if not data:
        st.info("Chưa có dữ liệu.")
        return pd.DataFrame()
    df = pd.DataFrame(data)
    existing = [col for col in columns if col in df.columns]
    df = df[existing].rename(columns=rename)
    st.dataframe(df, use_container_width=True, hide_index=True)
    return df


WATER_METRICS = [
    {"column": "temperature", "label": "Nhiệt độ", "unit": "°C", "min": 18, "max": 33, "color": "#c2410c"},
    {"column": "ph", "label": "pH", "unit": "", "min": 7.0, "max": 9.0, "color": "#1d4ed8"},
    {"column": "salinity", "label": "Độ mặn", "unit": "‰", "min": 5, "max": 35, "color": "#0f766e"},
    {"column": "doValue", "label": "Oxy hòa tan", "unit": "mg/L", "min": 3.5, "max": None, "color": "#7c3aed"},
    {"column": "ecValue", "label": "EC", "unit": "", "min": None, "max": None, "color": "#475569"},
]


def render_metric_chart(df: pd.DataFrame, metric: dict):
    column = metric["column"]
    if column not in df.columns:
        st.info(f"Chưa có dữ liệu {metric['label']}.")
        return

    plot_df = df[["createdAt", column, "finalStatus"]].dropna(subset=["createdAt", column]).copy()
    if plot_df.empty:
        st.info(f"Chưa có dữ liệu {metric['label']}.")
        return

    plot_df = plot_df.rename(columns={column: "value", "createdAt": "time", "finalStatus": "status"})
    y_title = metric["label"] if not metric["unit"] else f"{metric['label']} ({metric['unit']})"

    base = alt.Chart(plot_df).encode(
        x=alt.X("time:T", title="Thời gian", axis=alt.Axis(format="%H:%M")),
        y=alt.Y("value:Q", title=y_title, scale=alt.Scale(zero=False)),
        tooltip=[
            alt.Tooltip("time:T", title="Thời gian", format="%d/%m/%Y %H:%M:%S"),
            alt.Tooltip("value:Q", title=metric["label"], format=".2f"),
            alt.Tooltip("status:N", title="Trạng thái"),
        ],
    )

    layers = []
    if metric["min"] is not None and metric["max"] is not None:
        threshold = pd.DataFrame(
            {
                "min": [metric["min"]],
                "max": [metric["max"]],
                "label": ["Vùng an toàn"],
            }
        )
        band = (
            alt.Chart(threshold)
            .mark_rect(opacity=0.12, color="#10b981")
            .encode(y="min:Q", y2="max:Q", tooltip=alt.Tooltip("label:N", title="Ngưỡng"))
        )
        layers.append(band)
    elif metric["min"] is not None:
        rule_df = pd.DataFrame({"value": [metric["min"]], "label": ["Ngưỡng tối thiểu"]})
        rule = (
            alt.Chart(rule_df)
            .mark_rule(strokeDash=[6, 4], color="#b25f00")
            .encode(y="value:Q", tooltip=alt.Tooltip("label:N", title="Ngưỡng"))
        )
        layers.append(rule)

    line = base.mark_line(color=metric["color"], strokeWidth=3).properties(height=240)
    points = base.mark_circle(size=44, color=metric["color"], opacity=0.85)
    chart = alt.layer(*layers, line, points).resolve_scale(y="shared").interactive()
    st.altair_chart(chart, use_container_width=True)


def render_status_distribution(df: pd.DataFrame):
    if "finalStatus" not in df.columns:
        return
    status_df = (
        df["finalStatus"]
        .fillna("UNKNOWN")
        .value_counts()
        .rename_axis("status")
        .reset_index(name="count")
    )
    if status_df.empty:
        return
    status_df["label"] = status_df["status"].map(status_text)
    chart = (
        alt.Chart(status_df)
        .mark_bar(cornerRadiusTopLeft=4, cornerRadiusTopRight=4)
        .encode(
            x=alt.X("label:N", title="Trạng thái"),
            y=alt.Y("count:Q", title="Số lần đo"),
            color=alt.Color(
                "status:N",
                title="",
                scale=alt.Scale(
                    domain=["NORMAL", "WARNING", "DANGER", "UNKNOWN"],
                    range=["#087f5b", "#b25f00", "#b42318", "#64748b"],
                ),
            ),
            tooltip=[
                alt.Tooltip("label:N", title="Trạng thái"),
                alt.Tooltip("count:Q", title="Số lần đo"),
            ],
        )
        .properties(height=220)
    )
    st.altair_chart(chart, use_container_width=True)


def health_probe():
    backend, backend_error = get_api("/health")
    ai_base = st.session_state.ai_base_url.rstrip("/")
    ai, ai_error = request_json("GET", f"{ai_base}/health")
    backend_status = "ERROR" if backend_error else (unwrap(backend) or {}).get("status", "UP")
    ai_status = "ERROR" if ai_error else (ai or {}).get("status", "UP")
    return backend_status, ai_status


def render_sidebar():
    with st.sidebar:
        st.header("Tài khoản")
        if not st.session_state.token:
            with st.form("login_form"):
                username = st.text_input("Tên đăng nhập", value=st.session_state.username or "admin")
                password = st.text_input("Mật khẩu", type="password")
                submitted = st.form_submit_button("Đăng nhập", use_container_width=True)
                if submitted:
                    error = login(username, password)
                    if error:
                        st.error(error)
                    else:
                        st.success(f"Xin chào {st.session_state.username}")
                        st.rerun()
        else:
            st.success(f"Đang dùng: {st.session_state.username}")
            if st.button("Đăng xuất", use_container_width=True):
                st.session_state.token = ""
                st.session_state.username = ""
                st.rerun()

        st.divider()
        st.header("Ao nuôi")
        device_options = [st.session_state.device_id or DEFAULT_DEVICE_ID]
        if st.session_state.token:
            devices_payload, devices_error = get_api("/devices", token=st.session_state.token)
            if not devices_error:
                devices = unwrap(devices_payload) or []
                ids = [item.get("deviceId") for item in devices if item.get("deviceId")]
                device_options = list(dict.fromkeys(ids or device_options))
        st.selectbox("Thiết bị giám sát", options=device_options, key="device_id")

        if st.button("Làm mới dữ liệu", use_container_width=True):
            st.rerun()

        with st.expander("Cấu hình kết nối"):
            st.text_input("Backend API", key="api_base_url")
            st.text_input("AI service", key="ai_base_url")


def require_login() -> bool:
    if st.session_state.token:
        return True
    st.info("Vui lòng đăng nhập để xem dữ liệu ao nuôi.")
    return False


def render_overview():
    backend_status, ai_status = health_probe()
    device_id = st.session_state.device_id

    cols = st.columns(4)
    with cols[0]:
        card("Hệ thống", status_text(backend_status), "Backend", status_class(backend_status))
    with cols[1]:
        card("AI hỗ trợ", status_text(ai_status), "Dịch vụ phân tích", status_class(ai_status))
    with cols[2]:
        card("Thiết bị", device_id, "Đang theo dõi", "ok")
    with cols[3]:
        card("Cập nhật", datetime.now().strftime("%H:%M:%S"), "Thời gian màn hình", "ok")

    if not require_login():
        return

    dashboard, dashboard_error = get_api(
        "/dashboard/summary",
        token=st.session_state.token,
        params={"deviceId": device_id},
    )
    latest_state, latest_state_error = get_api(f"/devices/{device_id}/latest-state", token=st.session_state.token)

    if dashboard_error:
        st.error("Không lấy được dữ liệu tổng quan.")
        return

    data = unwrap(dashboard) or {}
    latest = data.get("latest") or {}
    state = unwrap(latest_state) if not latest_state_error else {}
    final_status = latest.get("finalStatus") or (state or {}).get("finalStatus") or "UNKNOWN"

    st.subheader("Tình trạng ao hiện tại")
    status_cols = st.columns(6)
    with status_cols[0]:
        card("Trạng thái", status_text(final_status), "Đánh giá tổng hợp", status_class(final_status))
    with status_cols[1]:
        card("Nhiệt độ", value_or_dash(latest.get("temperature"), " °C"), "18-33 °C", status_class(final_status))
    with status_cols[2]:
        card("pH", value_or_dash(latest.get("ph")), "7.0-9.0", status_class(final_status))
    with status_cols[3]:
        card("Độ mặn", value_or_dash(latest.get("salinity"), " ‰"), "5-35 ‰", status_class(final_status))
    with status_cols[4]:
        card("Oxy hòa tan", value_or_dash(latest.get("doValue"), " mg/L"), "Tối thiểu 3.5", status_class(final_status))
    with status_cols[5]:
        card("Cảnh báo", str(data.get("openAlertCount", 0)), "Đang mở", "bad" if data.get("openAlertCount", 0) else "ok")

    st.subheader("Thống kê dữ liệu")
    metric_cols = st.columns(4)
    metric_cols[0].metric("Tổng số lần đo", data.get("totalReadings", 0))
    metric_cols[1].metric("Ổn định", data.get("normalCount", 0))
    metric_cols[2].metric("Cần theo dõi", data.get("warningCount", 0))
    metric_cols[3].metric("Cần xử lý", data.get("dangerCount", 0))

    message = latest.get("message") or latest.get("aiMessage") or ""
    action = latest.get("recommendedAction") or ""
    if message or action:
        st.subheader("Gợi ý vận hành")
        if message:
            if status_class(final_status) != "ok":
                st.warning(message)
            else:
                st.success(message)
        if action:
            st.info(action)


def render_water_quality():
    if not require_login():
        return
    device_id = st.session_state.device_id
    st.subheader("Diễn biến chất lượng nước")
    limit = st.slider("Số lần đo gần nhất", min_value=20, max_value=300, value=100, step=20)
    payload, error = get_api(
        "/readings/history",
        token=st.session_state.token,
        params={"deviceId": device_id, "limit": limit},
    )
    if error:
        st.error("Không lấy được lịch sử cảm biến.")
        return

    rows = unwrap(payload) or []
    df = pd.DataFrame(rows)
    if df.empty:
        st.info("Chưa có dữ liệu cảm biến.")
        return

    df["createdAt"] = pd.to_datetime(df["createdAt"], errors="coerce")
    df = df.sort_values("createdAt")

    st.markdown("#### Biểu đồ theo dõi")
    with st.container(border=True):
        st.markdown("##### Trạng thái các lần đo")
        render_status_distribution(df)

    first_row = st.columns(2)
    for index, metric in enumerate(WATER_METRICS[:2]):
        with first_row[index]:
            with st.container(border=True):
                st.markdown(f"##### {metric['label']}")
                render_metric_chart(df, metric)

    second_row = st.columns(2)
    for index, metric in enumerate(WATER_METRICS[2:4]):
        with second_row[index]:
            with st.container(border=True):
                st.markdown(f"##### {metric['label']}")
                render_metric_chart(df, metric)

    with st.container(border=True):
        st.markdown("##### EC")
        render_metric_chart(df, WATER_METRICS[4])

    st.subheader("Các lần đo gần đây")
    friendly_table(
        df.sort_values("createdAt", ascending=False).to_dict("records"),
        {
            "createdAt": "Thời gian",
            "temperature": "Nhiệt độ",
            "ph": "pH",
            "ecValue": "EC",
            "salinity": "Độ mặn",
            "doValue": "Oxy hòa tan",
            "finalStatus": "Trạng thái",
            "recommendedAction": "Khuyến nghị",
        },
        ["createdAt", "temperature", "ph", "ecValue", "salinity", "doValue", "finalStatus", "recommendedAction"],
    )


def render_alerts():
    if not require_login():
        return
    device_id = st.session_state.device_id
    st.subheader("Cảnh báo")
    open_alerts, open_error = get_api("/alerts/open", token=st.session_state.token, params={"deviceId": device_id})
    history, history_error = get_api("/alerts/history", token=st.session_state.token, params={"deviceId": device_id})
    notifications, notif_error = get_api("/notifications", token=st.session_state.token, params={"deviceId": device_id})

    if open_error:
        st.error("Không lấy được cảnh báo đang mở.")
    else:
        open_rows = unwrap(open_alerts) or []
        if open_rows:
            st.error(f"Có {len(open_rows)} cảnh báo đang cần xử lý.")
        else:
            st.success("Không có cảnh báo đang mở.")
        friendly_table(
            open_rows,
            {
                "id": "Mã",
                "alertType": "Loại cảnh báo",
                "severity": "Mức độ",
                "status": "Trạng thái",
                "message": "Nội dung",
                "createdAt": "Thời gian",
            },
            ["id", "alertType", "severity", "status", "message", "createdAt"],
        )

    st.subheader("Lịch sử cảnh báo")
    if history_error:
        st.error("Không lấy được lịch sử cảnh báo.")
    else:
        friendly_table(
            unwrap(history),
            {
                "id": "Mã",
                "alertType": "Loại cảnh báo",
                "severity": "Mức độ",
                "status": "Trạng thái",
                "message": "Nội dung",
                "createdAt": "Tạo lúc",
                "resolvedAt": "Xử lý lúc",
            },
            ["id", "alertType", "severity", "status", "message", "createdAt", "resolvedAt"],
        )

    st.subheader("Thông báo trong hệ thống")
    if notif_error:
        st.error("Không lấy được thông báo.")
    else:
        friendly_table(
            unwrap(notifications),
            {
                "channel": "Kênh",
                "severity": "Mức độ",
                "status": "Trạng thái",
                "suppressed": "Đã chống lặp",
                "message": "Nội dung",
                "createdAt": "Thời gian",
            },
            ["channel", "severity", "status", "suppressed", "message", "createdAt"],
        )


def render_device():
    if not require_login():
        return
    device_id = st.session_state.device_id
    st.subheader("Thiết bị và relay")
    device, device_error = get_api(f"/devices/{device_id}", token=st.session_state.token)
    states, states_error = get_api(f"/relay-states/{device_id}", token=st.session_state.token)
    relays, relays_error = get_api(f"/devices/{device_id}/relays", token=st.session_state.token)
    commands, commands_error = get_api("/commands/history", token=st.session_state.token, params={"deviceId": device_id})

    if device_error:
        st.error("Không lấy được thông tin thiết bị.")
    else:
        info = unwrap(device) or {}
        info_cols = st.columns(4)
        with info_cols[0]:
            card("Tên thiết bị", info.get("name") or device_id, info.get("deviceId") or device_id, "ok")
        with info_cols[1]:
            card("Kết nối", status_text(info.get("connectionStatus")), "Trạng thái mạng", status_class(info.get("connectionStatus")))
        with info_cols[2]:
            card("Hoạt động", status_text(info.get("status")), "Trạng thái thiết bị", status_class(info.get("status")))
        with info_cols[3]:
            card("Vị trí", info.get("installationPosition") or "-", "Điểm lắp đặt", "ok")

    cols = st.columns(2)
    with cols[0]:
        st.markdown("#### Trạng thái relay")
        if states_error:
            st.error("Không lấy được trạng thái relay.")
        else:
            friendly_table(
                unwrap(states),
                {
                    "relayNo": "Relay",
                    "relayName": "Tên",
                    "currentState": "Trạng thái",
                    "lastCommandId": "Lệnh gần nhất",
                    "lastUpdatedAt": "Cập nhật",
                },
                ["relayNo", "relayName", "currentState", "lastCommandId", "lastUpdatedAt"],
            )
    with cols[1]:
        st.markdown("#### Thông tin relay")
        if relays_error:
            st.error("Không lấy được thông tin relay.")
        else:
            friendly_table(
                unwrap(relays),
                {
                    "relayNo": "Relay",
                    "name": "Tên",
                    "relayType": "Loại",
                    "status": "Trạng thái",
                    "locked": "Đang khóa",
                    "lockedBy": "Người khóa",
                },
                ["relayNo", "name", "relayType", "status", "locked", "lockedBy"],
            )

    st.subheader("Lịch sử điều khiển")
    if commands_error:
        st.error("Không lấy được lịch sử điều khiển.")
    else:
        friendly_table(
            unwrap(commands),
            {
                "id": "Mã lệnh",
                "relayNo": "Relay",
                "action": "Thao tác",
                "status": "Kết quả",
                "source": "Nguồn",
                "requestedBy": "Người tạo",
                "message": "Nội dung",
                "createdAt": "Tạo lúc",
                "ackAt": "Xác nhận lúc",
            },
            ["id", "relayNo", "action", "status", "source", "requestedBy", "message", "createdAt", "ackAt"],
        )


def render_ai_assistant():
    if not require_login():
        return

    device_id = st.session_state.device_id
    st.subheader("AI Assistant")

    if st.button("Bắt đầu cuộc trò chuyện mới", use_container_width=False):
        st.session_state.chat_session_id = None
        st.session_state.chat_messages = []
        st.rerun()

    for item in st.session_state.chat_messages:
        with st.chat_message(item["role"]):
            st.markdown(item["content"])

    prompt = st.chat_input("Hỏi về tình trạng ao, cảnh báo, relay hoặc kiến thức vận hành")
    if not prompt:
        return

    st.session_state.chat_messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.markdown(prompt)

    payload, error = request_json(
        "POST",
        f"{st.session_state.api_base_url.rstrip('/')}/chat/message",
        token=st.session_state.token,
        json_body={
            "sessionId": st.session_state.chat_session_id,
            "deviceId": device_id,
            "message": prompt,
        },
    )

    if error:
        answer = f"Không gửi được câu hỏi tới backend: {error}"
    else:
        data = unwrap(payload) or {}
        st.session_state.chat_session_id = data.get("sessionId")
        bot_message = data.get("botMessage") or {}
        answer = bot_message.get("content") or "Backend chưa trả về nội dung trả lời."

    st.session_state.chat_messages.append({"role": "assistant", "content": answer})
    with st.chat_message("assistant"):
        st.markdown(answer)


def main():
    init_state()
    render_sidebar()

    st.markdown("<h1 class='app-title'>IoT- Hệ thống giám sát môi trường ao nuôi thủy hải sản</h1>", unsafe_allow_html=True)
    st.markdown(
        "<div class='app-subtitle'>Theo dõi môi trường nước, cảnh báo và thiết bị trong ao nuôi.</div>",
        unsafe_allow_html=True,
    )

    tabs = st.tabs(["Tổng quan", "Chất lượng nước", "Cảnh báo", "Thiết bị", "AI Assistant"])
    with tabs[0]:
        render_overview()
    with tabs[1]:
        render_water_quality()
    with tabs[2]:
        render_alerts()
    with tabs[3]:
        render_device()
    with tabs[4]:
        render_ai_assistant()


if __name__ == "__main__":
    main()
