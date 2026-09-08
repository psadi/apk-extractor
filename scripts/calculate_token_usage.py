#!/usr/bin/env python3
"""
calculate_token_usage.py
Calculates exact Gemini token usage, thinking tokens, and tiered pricing expenditures
from Antigravity CLI conversation SQLite databases.
"""

import os
import sqlite3
import sys

def decode_varint(stream, pos):
    res = 0
    shift = 0
    while True:
        b = stream[pos]
        pos += 1
        res |= (b & 0x7F) << shift
        shift += 7
        if not (b & 0x80):
            break
    return res, pos

def parse_proto(data):
    pos = 0
    fields = {}
    while pos < len(data):
        tag, pos = decode_varint(data, pos)
        wire_type = tag & 0x7
        field_num = tag >> 3
        if wire_type == 0:
            val, pos = decode_varint(data, pos)
        elif wire_type == 1:
            val = data[pos:pos+8]
            pos += 8
        elif wire_type == 2:
            length, pos = decode_varint(data, pos)
            val = data[pos:pos+length]
            pos += length
        elif wire_type == 5:
            val = data[pos:pos+4]
            pos += 4
        else:
            raise ValueError(f"Unknown wire type {wire_type}")
        fields.setdefault(field_num, []).append((wire_type, val))
    return fields

def get_db_path():
    conv_id = os.environ.get("CONVERSATION_ID", "3cb70da8-b473-4182-95e4-a46b3b0a13c1")
    default_path = f"/data/data/com.termux/files/home/.gemini/antigravity-cli/conversations/{conv_id}.db"
    if os.path.exists(default_path):
        return default_path
    home = os.path.expanduser("~")
    alt_path = os.path.join(home, ".gemini", "antigravity-cli", "conversations", f"{conv_id}.db")
    if os.path.exists(alt_path):
        return alt_path
    return default_path

def main():
    db_path = sys.argv[1] if len(sys.argv) > 1 else get_db_path()
    if not os.path.exists(db_path):
        print(f"Error: Database not found at {db_path}", file=sys.stderr)
        sys.exit(1)

    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute("SELECT idx, data FROM gen_metadata ORDER BY idx ASC")
    rows = cur.fetchall()

    records = []
    for idx, data in rows:
        try:
            top = parse_proto(data)
            for f1_item in top.get(1, []):
                f1 = parse_proto(f1_item[1])
                for f1_4_item in f1.get(4, []):
                    f1_4 = parse_proto(f1_4_item[1])
                    prompt = f1_4.get(5, [(0, 0)])[0][1]
                    cached = f1_4.get(2, [(0, 0)])[0][1]
                    output = f1_4.get(3, [(0, 0)])[0][1]
                    thinking = f1_4.get(10, [(0, 0)])[0][1]
                    records.append({
                        "idx": idx,
                        "prompt": prompt,
                        "cached": cached,
                        "output": output,
                        "thinking": thinking
                    })
        except Exception:
            pass

    total_records = len(records)
    total_prompt = sum(r["prompt"] for r in records)
    total_cached = sum(r["cached"] for r in records)
    total_non_cached = total_prompt - total_cached
    total_output = sum(r["output"] for r in records)
    total_thinking = sum(r["thinking"] for r in records)
    total_code_tool = total_output - total_thinking
    total_tokens = total_prompt + total_output
    peak_context = max(r["prompt"] for r in records) if records else 0

    total_spend = 0.0
    for r in records:
        prompt = r["prompt"]
        cached = r["cached"]
        non_cached = max(0, prompt - cached)
        output = r["output"]
        if prompt <= 128000:
            total_spend += (non_cached * 0.075 / 1e6) + (cached * 0.01875 / 1e6) + (output * 0.30 / 1e6)
        else:
            total_spend += (non_cached * 0.15 / 1e6) + (cached * 0.0375 / 1e6) + (output * 0.60 / 1e6)

    print(f"Total Invocations: {total_records}")
    print(f"Total Input Tokens: {total_prompt:,} (~{total_prompt/1e6:.1f}M)")
    print(f"  Non-cached: {total_non_cached:,} ({(total_non_cached/total_prompt)*100:.1f}%)")
    print(f"  Cached: {total_cached:,} ({(total_cached/total_prompt)*100:.1f}%)")
    print(f"Total Output Tokens: {total_output:,} (~{total_output/1e3:.0f}K)")
    print(f"  Thinking Tokens: {total_thinking:,} ({(total_thinking/total_output)*100:.1f}%)")
    print(f"  Code & Tools: {total_code_tool:,} ({(total_code_tool/total_output)*100:.1f}%)")
    print(f"Total Tokens: {total_tokens:,} (~{total_tokens/1e6:.1f}M)")
    print(f"Peak Context Window: {peak_context:,}")
    print(f"Total AI Spend: ${total_spend:.2f} USD")

if __name__ == "__main__":
    main()
