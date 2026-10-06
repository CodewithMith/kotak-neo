from fastapi import FastAPI
import random

app = FastAPI()

@app.get("/market")
def get_market():
    return {
        "success": True,
        "nifty": {
            "symbol": "NIFTY 50",
            "ltp": 22450.75 + round(random.uniform(-10, 10), 2),
            "change": 125.40,
            "change_percent": 0.56
        },
        "sensex": {
            "symbol": "SENSEX",
            "ltp": 73880.20 + round(random.uniform(-30, 30), 2),
            "change": 390.15,
            "change_percent": 0.53
        },
        "stocks": [
            {"symbol": "RELIANCE", "name": "Reliance Industries Ltd", "ltp": 2980.50 + round(random.uniform(-5, 5), 2), "change": 35.20, "changePercent": 1.19},
            {"symbol": "TCS", "name": "Tata Consultancy Services", "ltp": 3950.00 + round(random.uniform(-5, 5), 2), "change": -18.50, "changePercent": -0.47},
            {"symbol": "INFY", "name": "Infosys Limited", "ltp": 1620.10 + round(random.uniform(-3, 3), 2), "change": 12.30, "changePercent": 0.77},
            {"symbol": "HDFCBANK", "name": "HDFC Bank Limited", "ltp": 1450.75 + round(random.uniform(-3, 3), 2), "change": -8.20, "changePercent": -0.56},
            {"symbol": "ICICIBANK", "name": "ICICI Bank Limited", "ltp": 1085.30 + round(random.uniform(-3, 3), 2), "change": 14.60, "changePercent": 1.36},
            {"symbol": "TATAMOTORS", "name": "Tata Motors Limited", "ltp": 975.40 + round(random.uniform(-3, 3), 2), "change": 22.80, "changePercent": 2.39},
            {"symbol": "SBIN", "name": "State Bank of India", "ltp": 760.25 + round(random.uniform(-2, 2), 2), "change": 5.10, "changePercent": 0.68},
            {"symbol": "BHARTIARTL", "name": "Bharti Airtel Ltd", "ltp": 1210.00 + round(random.uniform(-3, 3), 2), "change": -4.50, "changePercent": -0.37}
        ]
    }

@app.post("/login/1.0/tradeApiLogin")
def login():
    return {
        "success": True,
        "message": "Login successful",
        "token": "03ce2a5d-317e-4736-afc2-94ce8a8ef0ac"
    }
