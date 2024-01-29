package ru.redguy.testtask;

import com.binance.connector.client.SpotClient;
import com.binance.connector.client.impl.SpotClientImpl;
import com.google.gson.Gson;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

public class Task2 {
    public static Gson gson = new Gson();

    public static void main(String[] args) {
        //Напишите код программы получение АПИ котировок с биржи бинанс
        Scanner scanner = new Scanner(System.in);
        System.out.print("Origin current (BNB): ");
        String from = scanner.nextLine();
        System.out.print("Result current (BTC): ");
        String to = scanner.nextLine();
        SpotClient client = new SpotClientImpl();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("symbol", from.trim() + to.trim());
        Price res = gson.fromJson(client.createMarket().averagePrice(params), Price.class);
        System.out.println(res.price);
    }

    class Price {
        public int mins;
        public String price;
        public String closeTime;
    }
}
