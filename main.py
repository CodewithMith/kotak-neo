from fastapi import FastAPI
import requests
from datetime import datetime, timedelta

app = FastAPI()

def get_live_price(ticker):
    try:
        url = f"https://query1.finance.yahoo.com/v8/finance/chart/{ticker}?interval=1m"
        headers = {"User-Agent": "Mozilla/5.0"}
        res = requests.get(url, headers=headers, timeout=3)
        data = res.json()
        meta = data['chart']['result'][0]['meta']
        ltp = meta['regularMarketPrice']
        prev_close = meta['chartPreviousClose']
        change = round(ltp - prev_close, 2)
        change_percent = round((change / prev_close) * 100, 2)
        return ltp, change, change_percent
    except Exception as e:
        return None, None, None

@app.get("/market")
def get_market():
    nifty_ltp, nifty_chg, nifty_pct = get_live_price("^NSEI")
    sensex_ltp, sensex_chg, sensex_pct = get_live_price("^BSESN")

    if not nifty_ltp:
        nifty_ltp, nifty_chg, nifty_pct = 22776.00, 180.50, 0.80
    if not sensex_ltp:
        sensex_ltp, sensex_chg, sensex_pct = 75120.40, 520.10, 0.70

    # Generate Option Chain based on live Nifty Spot
    base_strike = round(nifty_ltp / 50.0) * 50.0
    strikes = [base_strike + (i * 50) for i in range(-5, 6)]

    option_chain = []
    for strike in strikes:
        diff = strike - nifty_ltp
        ce_price = max(5.0, round(max(0.0, nifty_ltp - strike) + 120.0 - (diff * 0.1), 2))
        pe_price = max(5.0, round(max(0.0, strike - nifty_ltp) + 120.0 + (diff * 0.1), 2))
        option_chain.append({
            "strikePrice": strike,
            "ceSymbol": f"NIFTY_CE_{int(strike)}",
            "ceLtp": ce_price,
            "ceChange": 1.5,
            "peSymbol": f"NIFTY_PE_{int(strike)}",
            "peLtp": pe_price,
            "peChange": -1.2
        })

    stock_tickers = {
        "RELIANCE": "RELIANCE.NS",
        "TCS": "TCS.NS",
        "INFY": "INFY.NS",
        "HDFCBANK": "HDFCBANK.NS",
        "ICICIBANK": "ICICIBANK.NS",
        "TATAMOTORS": "TATAMOTORS.NS",
        "SBIN": "SBIN.NS",
        "BHARTIARTL": "BHARTIARTL.NS"
    }

    stock_names = {
        "RELIANCE": "Reliance Industries Ltd",
        "TCS": "Tata Consultancy Services",
        "INFY": "Infosys Limited",
        "HDFCBANK": "HDFC Bank Limited",
        "ICICIBANK": "ICICI Bank Limited",
        "TATAMOTORS": "Tata Motors Limited",
        "SBIN": "State Bank of India",
        "BHARTIARTL": "Bharti Airtel Ltd"
    }

    stocks = []
    for symbol, ticker in stock_tickers.items():
        ltp, chg, pct = get_live_price(ticker)
        if ltp is None:
            if symbol == "RELIANCE": ltp, chg, pct = 2995.00, 45.20, 1.53
            elif symbol == "TCS": ltp, chg, pct = 3980.00, 12.50, 0.32
            elif symbol == "INFY": ltp, chg, pct = 1640.50, 22.30, 1.38
            elif symbol == "HDFCBANK": ltp, chg, pct = 1465.00, 15.00, 1.03
            elif symbol == "ICICIBANK": ltp, chg, pct = 1105.00, 18.60, 1.71
            elif symbol == "TATAMOTORS": ltp, chg, pct = 995.00, 32.80, 3.41
            elif symbol == "SBIN": ltp, chg, pct = 775.00, 12.10, 1.59
            else: ltp, chg, pct = 1230.00, 15.50, 1.27

        stocks.append({
            "symbol": symbol,
            "name": stock_names[symbol],
            "ltp": ltp,
            "change": chg,
            "changePercent": pct
        })

    return {
        "success": True,
        "nifty": {
            "symbol": "NIFTY 50",
            "ltp": nifty_ltp,
            "change": nifty_chg,
            "change_percent": nifty_pct
        },
        "sensex": {
            "symbol": "SENSEX",
            "ltp": sensex_ltp,
            "change": sensex_chg,
            "change_percent": sensex_pct
        },
        "stocks": stocks,
        "optionChain": option_chain
    }

@app.post("/login/1.0/tradeApiLogin")
def login():
    return {
        "success": True,
        "message": "Login successful",
        "token": "03ce2a5d-317e-4736-afc2-94ce8a8ef0ac"
    }
