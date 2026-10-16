#!/usr/bin/env python3
"""
Generates mock fitness-app export files for demo/dev users, standing in
for a real fitness-app integration (see Use Case: Fitness-Based Calorie
Tracking). Writes one JSON file per username into fitness-data/, in the
shape FitnessDataService expects: {username, date, caloriesBurned, steps,
activeMinutes}.

Usage:
  python3 scripts/generate_fitness_data.py demo_customer
  python3 scripts/generate_fitness_data.py demo_customer other_user --seed 42
"""
import argparse
import datetime
import json
import os
import random

OUTPUT_DIR = os.path.join(os.path.dirname(__file__), "..", "fitness-data")


def generate_one(username: str, rng: random.Random) -> dict:
    steps = rng.randint(2000, 12000)
    active_minutes = rng.randint(10, 90)
    # Rough, plausible relationship: more steps/active time burns more calories.
    calories_burned = int(steps * 0.04 + active_minutes * 3 + rng.randint(-30, 30))
    calories_burned = max(calories_burned, 0)

    return {
        "username": username,
        "date": datetime.date.today().isoformat(),
        "caloriesBurned": calories_burned,
        "steps": steps,
        "activeMinutes": active_minutes,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("usernames", nargs="+", help="Usernames to generate fitness data for")
    parser.add_argument("--seed", type=int, default=None, help="Random seed, for reproducible demo data")
    args = parser.parse_args()

    rng = random.Random(args.seed)
    os.makedirs(OUTPUT_DIR, exist_ok=True)

    for username in args.usernames:
        data = generate_one(username, rng)
        out_path = os.path.join(OUTPUT_DIR, f"{username}.json")
        with open(out_path, "w") as f:
            json.dump(data, f, indent=2)
        print(f"Wrote {out_path}: {data}")


if __name__ == "__main__":
    main()
