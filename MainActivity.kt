package com.example.kotakneo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: PaperTradingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF1E88E5),
                    secondary = Color(0xFF26A69A),
                    background = Color(0xFF121212),
                    surface = Color(0xFF1E1E1E),
                    onSurface = Color.White
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PaperTradingApp(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperTradingApp(viewModel: PaperTradingViewModel) {

    val context = LocalContext.current

    val wallet by viewModel.wallet.collectAsState()
    val positions by viewModel.positions.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val nifty by viewModel.nifty.collectAsState()
    val sensex by viewModel.sensex.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }

    var selectedStockForTrade by remember { mutableStateOf<StockItem?>(null) }
    var initialOrderType by remember { mutableStateOf(OrderType.BUY) }

    val totalInvested = positions.sumOf { it.investedAmount }
    val totalCurrentValue = positions.sumOf { it.currentAmount }
    val totalPnl = totalCurrentValue - totalInvested
    val totalPnlPercent = if (totalInvested > 0) (totalPnl / totalInvested) * 100 else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kotak Neo Paper Trading",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.refreshMarket() },
                        enabled = !isLoading
                    ) {
                        Text(
                            text = if (isLoading) "Updating..." else "REFRESH",
                            color = Color(0xFF64B5F6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1F1F1F)
                )
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            WalletSummaryCard(
                availableCash = wallet.availableCash,
                invested = totalInvested,
                currentValue = totalCurrentValue,
                totalPnl = totalPnl,
                totalPnlPercent = totalPnlPercent
            )

            Spacer(modifier = Modifier.height(8.dp))

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("WATCHLIST (${watchlist.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("POSITIONS (${positions.size})", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("ORDERS (${orders.size})", fontWeight = FontWeight.SemiBold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> WatchlistScreen(
                    nifty = nifty,
                    sensex = sensex,
                    stocks = watchlist,
                    onBuyClick = { stock ->
                        selectedStockForTrade = stock
                        initialOrderType = OrderType.BUY
                    },
                    onSellClick = { stock ->
                        selectedStockForTrade = stock
                        initialOrderType = OrderType.SELL
                    }
                )

                1 -> PositionsScreen(
                    positions = positions,
                    onSellClick = { pos ->
                        val stockItem = StockItem(
                            symbol = pos.symbol,
                            name = pos.symbol,
                            ltp = pos.currentLtp,
                            change = 0.0,
                            changePercent = 0.0
                        )
                        selectedStockForTrade = stockItem
                        initialOrderType = OrderType.SELL
                    }
                )

                2 -> OrdersScreen(orders = orders)
            }
        }
    }

    selectedStockForTrade?.let { stock ->
        TradeDialog(
            stock = stock,
            initialOrderType = initialOrderType,
            availableCash = wallet.availableCash,
            onDismiss = { selectedStockForTrade = null },
            onConfirmTrade = { orderType, quantity, price ->
                val result = if (orderType == OrderType.BUY) {
                    viewModel.buyStock(stock.symbol, quantity, price)
                } else {
                    viewModel.sellStock(stock.symbol, quantity, price)
                }

                Toast.makeText(context, result.second, Toast.LENGTH_LONG).show()
                if (result.first) {
                    selectedStockForTrade = null
                }
            }
        )
    }
}

@Composable
fun WalletSummaryCard(
    availableCash: Double,
    invested: Double,
    currentValue: Double,
    totalPnl: Double,
    totalPnlPercent: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF252528)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VIRTUAL WALLET CASH",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", availableCash)}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TOTAL P&L",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    val pnlColor = if (totalPnl >= 0) Color(0xFF4CAF50) else Color(0xFFEF5350)
                    val sign = if (totalPnl >= 0) "+" else ""
                    Text(
                        text = "$sign₹${String.format(Locale.US, "%.2f", totalPnl)} ($sign${String.format(Locale.US, "%.2f", totalPnlPercent)}%)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = pnlColor
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color(0xFF383838)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Invested", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", invested)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = "Current Value", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Text(
                        text = "₹${String.format(Locale.US, "%,.2f", currentValue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun WatchlistScreen(
    nifty: IndexData?,
    sensex: IndexData?,
    stocks: List<StockItem>,
    onBuyClick: (StockItem) -> Unit,
    onSellClick: (StockItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                nifty?.let {
                    IndexCard(modifier = Modifier.weight(1f), data = it)
                }
                sensex?.let {
                    IndexCard(modifier = Modifier.weight(1f), data = it)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(stocks) { stock ->
            StockItemRow(
                stock = stock,
                onBuyClick = { onBuyClick(stock) },
                onSellClick = { onSellClick(stock) }
            )
        }
    }
}

@Composable
fun IndexCard(modifier: Modifier = Modifier, data: IndexData) {
    val isPositive = data.change >= 0
    val color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val sign = if (isPositive) "+" else ""

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E24)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = data.symbol, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "₹${String.format(Locale.US, "%,.2f", data.ltp)}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "$sign${String.format(Locale.US, "%.2f", data.change)} ($sign${String.format(Locale.US, "%.2f", data.change_percent)}%)",
                fontSize = 12.sp,
                color = color
            )
        }
    }
}

@Composable
fun StockItemRow(
    stock: StockItem,
    onBuyClick: () -> Unit,
    onSellClick: () -> Unit
) {
    val isPositive = stock.change >= 0
    val changeColor = if (isPositive) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val sign = if (isPositive) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stock.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = stock.name,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Text(
                    text = "₹${String.format(Locale.US, "%,.2f", stock.ltp)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = "$sign${String.format(Locale.US, "%.2f", stock.change)} ($sign${String.format(Locale.US, "%.2f", stock.changePercent)}%)",
                    fontSize = 12.sp,
                    color = changeColor
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onBuyClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("BUY", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("SELL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PositionsScreen(
    positions: List<Position>,
    onSellClick: (Position) -> Unit
) {
    if (positions.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "कोई एक्टिव पोजीशन/होल्डिंग नहीं है!\nWatchlist से शेयर खरीदें।",
                color = Color.Gray,
                fontSize = 16.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(positions) { pos ->
                PositionRow(pos = pos, onSellClick = { onSellClick(pos) })
            }
        }
    }
}

@Composable
fun PositionRow(
    pos: Position,
    onSellClick: () -> Unit
) {
    val isProfit = pos.pnl >= 0
    val pnlColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFEF5350)
    val sign = if (isProfit) "+" else ""

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = pos.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = Color(0xFF2C2C2C),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Qty: ${pos.quantity}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$sign₹${String.format(Locale.US, "%.2f", pos.pnl)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = pnlColor
                    )
                    Text(
                        text = "$sign${String.format(Locale.US, "%.2f", pos.pnlPercent)}%",
                        fontSize = 12.sp,
                        color = pnlColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Avg Buy: ₹${String.format(Locale.US, "%.2f", pos.averagePrice)} | LTP: ₹${String.format(Locale.US, "%.2f", pos.currentLtp)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Current Value: ₹${String.format(Locale.US, "%,.2f", pos.currentAmount)}",
                        fontSize = 13.sp,
                        color = Color.LightGray
                    )
                }

                Button(
                    onClick = onSellClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("EXIT", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun OrdersScreen(orders: List<Order>) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "कोई ऑर्डर्स की हिस्ट्री नहीं है।",
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(orders) { order ->
                OrderRow(order = order)
            }
        }
    }
}

@Composable
fun OrderRow(order: Order) {
    val isBuy = order.orderType == OrderType.BUY
    val badgeColor = if (isBuy) Color(0xFF2E7D32) else Color(0xFFC62828)
    val timeFormat = SimpleDateFormat("hh:mm:ss a, dd MMM", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(order.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (isBuy) "BUY" else "SELL",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = order.symbol,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTime,
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${order.quantity} x ₹${String.format(Locale.US, "%.2f", order.price)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Total: ₹${String.format(Locale.US, "%,.2f", order.totalAmount)}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun TradeDialog(
    stock: StockItem,
    initialOrderType: OrderType,
    availableCash: Double,
    onDismiss: () -> Unit,
    onConfirmTrade: (OrderType, Int, Double) -> Unit
) {
    var orderType by remember { mutableStateOf(initialOrderType) }
    var quantityText by remember { mutableStateOf("1") }

    val quantity = quantityText.toIntOrNull() ?: 0
    val totalEstimated = quantity * stock.ltp

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF222226),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = stock.symbol, fontWeight = FontWeight.Bold)
                Text(text = "LTP: ₹${stock.ltp}", fontSize = 14.sp, color = Color(0xFF64B5F6))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { orderType = OrderType.BUY },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (orderType == OrderType.BUY) Color(0xFF2E7D32) else Color(0xFF333333)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("BUY")
                    }

                    Button(
                        onClick = { orderType = OrderType.SELL },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (orderType == OrderType.SELL) Color(0xFFC62828) else Color(0xFF333333)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SELL")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { char -> char.isDigit() } },
                    label = { Text("मात्रा (Quantity)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "कुल अनुमानित राशि: ₹${String.format(Locale.US, "%,.2f", totalEstimated)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )

                Text(
                    text = "उपलब्ध कैश: ₹${String.format(Locale.US, "%,.2f", availableCash)}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (quantity > 0) {
                        onConfirmTrade(orderType, quantity, stock.ltp)
                    }
                },
                enabled = quantity > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (orderType == OrderType.BUY) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            ) {
                Text(if (orderType == OrderType.BUY) "BUY ORDER पुष्टि करें" else "SELL ORDER पुष्टि करें")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("रद्द करें (Cancel)", color = Color.Gray)
            }
        }
    )
}
