package client;

import com.google.gwt.core.client.*;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Random;
import com.google.gwt.i18n.client.*;
import com.google.gwt.user.client.rpc.*;

import java.util.*;

/**
 * The Main GWT module implementation
 */
public class StockWatcher implements EntryPoint {
    
    static final int REFRESH_INTERVAL = 5000; // ms
    StockPriceServiceAsync stockPriceSvc = GWT.create(StockPriceService.class);
    
    VerticalPanel mainPanel = new VerticalPanel();
    FlexTable stocksFlexTable = new FlexTable();
    HorizontalPanel addPanel = new HorizontalPanel();
    TextBox newSymbolTextBox = new TextBox();
    Button addStockButton = new Button("Add");
    Label lastUpdatedLabel = new Label();

    public void onModuleLoad() {
        // Create table for stock data.
        stocksFlexTable.setText(0, 0, "Symbol");
        stocksFlexTable.setText(0, 1, "Price");
        stocksFlexTable.setText(0, 2, "Change");
        stocksFlexTable.setText(0, 3, "Remove");
        
        // Add styles to elements in the stock list table.
        stocksFlexTable.setCellPadding(6);
        
        // Add styles to elements in the stock list table.
        stocksFlexTable.getRowFormatter().addStyleName(0, "watchListHeader");
        stocksFlexTable.addStyleName("watchList");
        stocksFlexTable.getCellFormatter().addStyleName(0, 1, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(0, 2, "watchListNumericColumn");
        stocksFlexTable.getCellFormatter().addStyleName(0, 3, "watchListRemoveColumn");
        
        // Assemble Add Stock panel.
        newSymbolTextBox.addStyleName("newSymbolTextBox");
        addStockButton.addStyleName("addStockButton");
        addPanel.add(newSymbolTextBox);
        addPanel.add(addStockButton);
        addPanel.addStyleName("addPanel");

        // Assemble Main panel.
        mainPanel.add(stocksFlexTable);
        mainPanel.add(addPanel);
        mainPanel.add(lastUpdatedLabel);

        // Associate the Main panel with the HTML host page.
        RootPanel.get("stockList").add(mainPanel);
        
        // Move cursor focus to the input box.
        newSymbolTextBox.setFocus(true);
        
        // First refresh
        refreshWatchList();
        
        // Setup timer to refresh list automatically.
        Timer refreshTimer = new Timer() {
            @Override
            public void run() {
                refreshWatchList();
            }
        };
        refreshTimer.scheduleRepeating(REFRESH_INTERVAL);
        
        // Listen for mouse events on the Add button.
        addStockButton.addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                addStock();
            }
        });
        
        // Listen for keyboard events in the input box.
        newSymbolTextBox.addKeyPressHandler(new KeyPressHandler() {
            public void onKeyPress(KeyPressEvent event) {
                if (event.getCharCode() == KeyCodes.KEY_ENTER) {
                    addStock();
                }
            }
        });
   
    }
    
    /**
     * Add stock to FlexTable. Executed when the user clicks the addStockButton or
     * presses enter in the newSymbolTextBox.
     */
    private void addStock() {
        final String symbol = newSymbolTextBox.getText().toUpperCase().trim();
        newSymbolTextBox.setFocus(true);

        // Stock code must be between 1 and 10 chars that are numbers, letters, or dots.
        if (!symbol.matches("^[0-9a-zA-Z\\.]{1,10}$")) {
          Window.alert("'" + symbol + "' is not a valid symbol.");
          newSymbolTextBox.selectAll();
          return;
        }
        
        // Initialize the service proxy.
        if (stockPriceSvc == null) {
            stockPriceSvc = GWT.create(StockPriceService.class);
        }

        // Set up the callback object.
        AsyncCallback<Void> callback = new AsyncCallback<Void>() {
            
            public void onFailure(Throwable caught) {
                Window.alert("Oops, cannot contact the Play server ...");
            }

            public void onSuccess(Void result) {
                // Get the stock price.
                refreshWatchList();
            }
            
        };

        // Make the call to the stock price service.
        stockPriceSvc.addSymbol(symbol, callback);

    }
    
    private void refreshWatchList() {
        // Initialize the service proxy.
        if (stockPriceSvc == null) {
            stockPriceSvc = GWT.create(StockPriceService.class);
        }

        // Set up the callback object.
        AsyncCallback<StockPrice[]> callback = new AsyncCallback<StockPrice[]>() {
            
            public void onFailure(Throwable caught) {
                Window.alert("Oops, cannot contact the Play server ...");
            }

            public void onSuccess(StockPrice[] result) {
                updateTable(result);
            }
            
        };

        // Make the call to the stock price service.
        stockPriceSvc.getStocks(callback);

    }
    
    private void updateTable(StockPrice[] stocks) {
        
        // Clean table
        while(stocksFlexTable.getRowCount() > 1) {
            stocksFlexTable.removeRow(1);
        }
        
        // Display symbols
        for (int i = 0; i < stocks.length; i++) {
            
            final String symbolToDelete = stocks[i].symbol;
            
            // Add the stock to the table.
            int row = stocksFlexTable.getRowCount();
            stocksFlexTable.setText(row, 0, stocks[i].symbol);
            stocksFlexTable.setWidget(row, 2, new Label());
            stocksFlexTable.getCellFormatter().addStyleName(row, 1, "watchListNumericColumn");
            stocksFlexTable.getCellFormatter().addStyleName(row, 2, "watchListNumericColumn");
            stocksFlexTable.getCellFormatter().addStyleName(row, 3, "watchListRemoveColumn");
            stocksFlexTable.getCellFormatter().addStyleName(row, 0, "symbol-"+symbolToDelete);
            stocksFlexTable.getCellFormatter().addStyleName(row, 1, "price-"+symbolToDelete);
            stocksFlexTable.getCellFormatter().addStyleName(row, 2, "change-"+symbolToDelete);
            
            // Add a button to remove this stock from the table.
            Button removeStockButton = new Button("x");
            removeStockButton.addStyleName("removeButton-"+symbolToDelete);
            removeStockButton.addStyleDependentName("remove");
            removeStockButton.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    // Set up the callback object.
                    AsyncCallback<Void> callback = new AsyncCallback<Void>() {

                        public void onFailure(Throwable caught) {
                            Window.alert("Oops, cannot contact the Play server ...");
                        }

                        public void onSuccess(Void result) {
                            // Get the stock price.
                            refreshWatchList();
                        }

                    };

                    // Make the call to the stock price service.
                    stockPriceSvc.removeSymbol(symbolToDelete, callback);
                    
                }
            });
            stocksFlexTable.setWidget(row, 3, removeStockButton);
            
            // Format the data in the Price and Change fields.
            String priceText = NumberFormat.getFormat("#,##0.00").format(stocks[i].price);
            NumberFormat changeFormat = NumberFormat.getFormat("+#,##0.00;-#,##0.00");
            String changeText = changeFormat.format(stocks[i].change);
            String changePercentText = changeFormat.format(stocks[i].getChangePercent());

            // Populate the Price and Change fields with new data.
            stocksFlexTable.setText(row, 1, priceText);
            Label changeWidget = (Label)stocksFlexTable.getWidget(row, 2);
            changeWidget.setText(changeText + " (" + changePercentText + "%)");

            // Change the color of text in the Change field based on its value.
            String changeStyleName = "noChange";
            if (stocks[i].getChangePercent() < -0.1f) {
                changeStyleName = "negativeChange";
            } else if (stocks[i].getChangePercent() > 0.1f) {
                changeStyleName = "positiveChange";
            }

            changeWidget.setStyleName(changeStyleName);
        }   
         
        // Display timestamp showing last refresh.
        lastUpdatedLabel.setText("Last update : " + DateTimeFormat.getMediumDateTimeFormat().format(new Date()));            
    }
    
}