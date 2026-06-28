#!/usr/bin/env python3
"""
Performance probe for the Shrimp IoT backend.

The script uses only Python standard-library modules. It publishes telemetry
through MQTT QoS 1, polls the REST API as the web/app side, and prints a
Markdown table that can be pasted into a report.
"""

from __future__ import annotations

import argparse
import json
import math
import socket
import struct
import sys
import time
import urllib.error
import urllib.request
from datetime import datetime
from typing import Any


def percentile(values: list[float], p: float) -> float:
    if not values:
        return float("nan")
    ordered = sorted(values)
    if len(ordered) == 1:
        return ordered[0]
    rank = (len(ordered) - 1) * p
    lower = math.floor(rank)
    upper = math.ceil(rank)
    if lower == upper:
        return ordered[lower]
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (rank - lower)


def median(values: list[float]) -> float:
    return percentile(values, 0.5)


def parse_backend_datetime(value: str) -> float:
    # Spring returns LocalDateTime, usually like 2026-06-25T22:10:30.123456.
    return datetime.fromisoformat(value).timestamp()


def json_request(
    method: str,
    url: str,
    body: dict[str, Any] | None = None,
    headers: dict[str, str] | None = None,
    timeout: float = 10.0,
) -> dict[str, Any]:
    data = None if body is None else json.dumps(body).encode("utf-8")
    request_headers = {"Content-Type": "application/json"}
    if headers:
        request_headers.update(headers)
    req = urllib.request.Request(url, data=data, headers=request_headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as response:
            raw = response.read().decode("utf-8")
            return json.loads(raw)
    except urllib.error.HTTPError as exc:
        raw = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"{method} {url} failed: HTTP {exc.code}: {raw}") from exc


def encode_remaining_length(value: int) -> bytes:
    encoded = bytearray()
    while True:
        digit = value % 128
        value //= 128
        if value > 0:
            digit |= 0x80
        encoded.append(digit)
        if value == 0:
            return bytes(encoded)


def encode_utf8(value: str) -> bytes:
    raw = value.encode("utf-8")
    return struct.pack("!H", len(raw)) + raw


def read_exact(sock: socket.socket, count: int) -> bytes:
    chunks = bytearray()
    while len(chunks) < count:
        chunk = sock.recv(count - len(chunks))
        if not chunk:
            raise ConnectionError("MQTT socket closed")
        chunks.extend(chunk)
    return bytes(chunks)


def read_packet(sock: socket.socket) -> tuple[int, bytes]:
    first = read_exact(sock, 1)[0]
    multiplier = 1
    remaining = 0
    while True:
        digit = read_exact(sock, 1)[0]
        remaining += (digit & 127) * multiplier
        if (digit & 128) == 0:
            break
        multiplier *= 128
    return first, read_exact(sock, remaining)


class MqttClient:
    def __init__(self, host: str, port: int, client_id: str, username: str = "", password: str = ""):
        self.host = host
        self.port = port
        self.client_id = client_id
        self.username = username
        self.password = password
        self.sock: socket.socket | None = None
        self.packet_id = 1

    def connect(self) -> None:
        sock = socket.create_connection((self.host, self.port), timeout=10)
        variable = encode_utf8("MQTT") + bytes([4])
        flags = 0x02
        payload = encode_utf8(self.client_id)
        if self.username:
            flags |= 0x80
            payload += encode_utf8(self.username)
        if self.password:
            flags |= 0x40
            payload += encode_utf8(self.password)
        variable += bytes([flags]) + struct.pack("!H", 30)
        packet = bytes([0x10]) + encode_remaining_length(len(variable) + len(payload)) + variable + payload
        sock.sendall(packet)
        packet_type, body = read_packet(sock)
        if packet_type != 0x20 or len(body) < 2 or body[1] != 0:
            raise RuntimeError(f"MQTT CONNACK failed: packet={packet_type:#x}, body={body!r}")
        self.sock = sock

    def publish_qos1(self, topic: str, payload: str) -> float:
        if self.sock is None:
            raise RuntimeError("MQTT client is not connected")
        pid = self.packet_id
        self.packet_id += 1
        variable = encode_utf8(topic) + struct.pack("!H", pid)
        raw_payload = payload.encode("utf-8")
        packet = bytes([0x32]) + encode_remaining_length(len(variable) + len(raw_payload)) + variable + raw_payload
        sent_at = time.perf_counter()
        self.sock.sendall(packet)
        while True:
            packet_type, body = read_packet(self.sock)
            if packet_type == 0x40 and len(body) == 2 and struct.unpack("!H", body)[0] == pid:
                return sent_at

    def disconnect(self) -> None:
        if self.sock:
            try:
                self.sock.sendall(bytes([0xE0, 0x00]))
            finally:
                self.sock.close()
                self.sock = None


def login(base_url: str, username: str, password: str) -> str:
    response = json_request(
        "POST",
        f"{base_url}/api/auth/login",
        {"username": username, "password": password},
    )
    token = response.get("data", {}).get("token")
    if not token:
        raise RuntimeError("Login response did not contain a token")
    return token


def get_latest(base_url: str, token: str, device_id: str) -> tuple[dict[str, Any], float, float]:
    start = time.perf_counter()
    response = json_request(
        "GET",
        f"{base_url}/api/readings/latest?deviceId={device_id}",
        headers={"Authorization": f"Bearer {token}"},
    )
    end = time.perf_counter()
    end_wall = time.time()
    return response.get("data") or {}, end - start, end_wall


def wait_for_reading(
    base_url: str,
    token: str,
    device_id: str,
    expected_temperature: float,
    timeout_seconds: float,
) -> tuple[dict[str, Any], float, float]:
    deadline = time.perf_counter() + timeout_seconds
    while time.perf_counter() < deadline:
        latest, rest_latency, end_wall = get_latest(base_url, token, device_id)
        if latest and abs(float(latest.get("temperature", -9999.0)) - expected_temperature) < 0.000001:
            return latest, rest_latency, end_wall
        time.sleep(0.05)
    raise TimeoutError(f"Timed out waiting for telemetry temperature={expected_temperature}")


def run_telemetry(args: argparse.Namespace) -> dict[str, Any]:
    token = login(args.base_url, args.username, args.password)
    mqtt = MqttClient(args.mqtt_host, args.mqtt_port, f"perf-{int(time.time())}", args.mqtt_username, args.mqtt_password)
    mqtt.connect()

    t21_ms: list[float] = []
    t32_ms: list[float] = []
    t31_ms: list[float] = []
    rest_ms: list[float] = []
    saved_ids: list[int] = []
    sent = 0
    saved = 0
    start_wall = time.time()

    try:
        for index in range(args.count):
            temperature = round(args.temperature_base + (index / 10000.0), 4)
            payload = {
                "deviceId": args.device_id,
                "temperature": temperature,
                "ph": 7.6,
                "ecValue": 12.0 + (index % 10),
                "salinity": 15.0,
                "doValue": 6.0,
            }
            t1_wall = time.time()
            mqtt.publish_qos1(args.telemetry_topic, json.dumps(payload, separators=(",", ":")))
            sent += 1
            try:
                latest, latest_rest_latency, t3_wall = wait_for_reading(
                    args.base_url,
                    token,
                    args.device_id,
                    temperature,
                    args.poll_timeout,
                )
            except TimeoutError:
                continue

            saved += 1
            saved_ids.append(int(latest["id"]))
            t2_wall = parse_backend_datetime(latest["createdAt"])
            t21_ms.append(max(0.0, (t2_wall - t1_wall) * 1000.0))
            t32_ms.append(max(0.0, (t3_wall - t2_wall) * 1000.0))
            t31_ms.append(max(0.0, (t3_wall - t1_wall) * 1000.0))
            rest_ms.append(latest_rest_latency * 1000.0)

            if args.interval_seconds > 0:
                time.sleep(args.interval_seconds)
    finally:
        mqtt.disconnect()

    elapsed_minutes = max((time.time() - start_wall) / 60.0, 0.000001)
    duplicate_count = len(saved_ids) - len(set(saved_ids))
    return {
        "sent": sent,
        "saved": saved,
        "lost_pct": (sent - saved) * 100.0 / sent if sent else 0.0,
        "duplicate_pct": duplicate_count * 100.0 / saved if saved else 0.0,
        "throughput_per_minute": saved / elapsed_minutes,
        "t21_median_ms": median(t21_ms),
        "t21_p59_ms": percentile(t21_ms, 0.59),
        "t32_median_ms": median(t32_ms),
        "t32_p59_ms": percentile(t32_ms, 0.59),
        "t31_p59_ms": percentile(t31_ms, 0.59),
        "rest_median_ms": median(rest_ms),
    }


def format_number(value: float, decimals: int = 1) -> str:
    if math.isnan(value):
        return "N/A"
    return f"{value:.{decimals}f}"


def print_report(args: argparse.Namespace, results: dict[str, Any]) -> None:
    offline_avg = args.offline_timeout_seconds + (args.offline_check_ms / 1000.0) / 2.0
    offline_p59 = args.offline_timeout_seconds + (args.offline_check_ms / 1000.0) * 0.59
    print("| Chi tieu | Ket qua do | Dieu kien do |")
    print("|---|---:|---|")
    print(f"| Do tre (t2-t1), trung vi | {format_number(results['t21_median_ms'])} ms | Toi thieu {args.count} ban tin, mang noi bo, MQTT QoS 1 |")
    print(f"| Do tre (t2-t1), p59 | {format_number(results['t21_p59_ms'])} ms | Toi thieu {args.count} ban tin, mang noi bo, MQTT QoS 1 |")
    print(f"| Do tre (t3-t2), trung vi | {format_number(results['t32_median_ms'])} ms | Backend REST API den web/app |")
    print(f"| Do tre (t3-t2), p59 | {format_number(results['t32_p59_ms'])} ms | Backend REST API den web/app |")
    print(f"| Do tre end-to-end (t3-t1), p59 | {format_number(results['t31_p59_ms'])} ms | Toan bo luong thiet bi den giao dien |")
    print(f"| Thong luong xu ly on dinh | {format_number(results['throughput_per_minute'])} ban tin/phut | K=1, I={args.interval_seconds} giay, T={format_number((args.count * max(args.interval_seconds, 0.0)) / 60.0, 2)} phut |")
    print(f"| Tong so ban tin gui | {results['sent']} | Du lieu tu thiet bi mo phong |")
    print(f"| Tong so ban tin luu thanh cong | {results['saved']} | Doi chieu qua REST latest theo gia tri nhiet do duy nhat |")
    print(f"| Ty le mat ban tin | {format_number(results['lost_pct'], 2)}% | QoS 1 |")
    print(f"| Ty le tin trung | {format_number(results['duplicate_pct'], 2)}% | Kiem tra theo id ban ghi quan sat duoc |")
    print(f"| Thoi gian phat hien offline trung binh | {format_number(offline_avg, 1)} giay | Timeout cau hinh {args.offline_timeout_seconds} giay, scheduler {args.offline_check_ms} ms |")
    print(f"| Thoi gian phat hien offline p59 | {format_number(offline_p59, 1)} giay | Gia tri tinh theo chu ky scheduler; nen lap lai toi thieu 5 lan de do thuc nghiem |")
    print("| Do tre command den ACK, trung vi/p59 | Chua do bang script nay | Can chay kem thiet bi/subscriber ACK MQTT |")
    print("| CPU/RAM lon nhat cua backend | Chua do bang script nay | Lay bang docker stats hoac Task Manager trong thoi gian thu tai |")
    print("| CPU/RAM lon nhat cua Mosquitto | Chua do bang script nay | Lay bang docker stats hoac Task Manager trong thoi gian thu tai |")


def main() -> int:
    parser = argparse.ArgumentParser(description="Run Shrimp IoT performance probe")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--mqtt-host", default="127.0.0.1")
    parser.add_argument("--mqtt-port", type=int, default=1883)
    parser.add_argument("--mqtt-username", default="")
    parser.add_argument("--mqtt-password", default="")
    parser.add_argument("--device-id", default="device_01")
    parser.add_argument("--telemetry-topic", default="shrimp-iot/devices/device_01/telemetry")
    parser.add_argument("--username", default="admin")
    parser.add_argument("--password", default="admin123")
    parser.add_argument("--count", type=int, default=500)
    parser.add_argument("--interval-seconds", type=float, default=0.1)
    parser.add_argument("--poll-timeout", type=float, default=5.0)
    parser.add_argument("--temperature-base", type=float, default=24.0)
    parser.add_argument("--offline-timeout-seconds", type=float, default=60.0)
    parser.add_argument("--offline-check-ms", type=float, default=30000.0)
    args = parser.parse_args()

    if args.count < 1:
        raise SystemExit("--count must be >= 1")
    results = run_telemetry(args)
    print_report(args, results)
    return 0


if __name__ == "__main__":
    sys.exit(main())
