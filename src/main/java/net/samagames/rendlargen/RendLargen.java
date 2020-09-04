package net.samagames.rendlargen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.samagames.persistanceapi.GameServiceManager;
import net.samagames.rendlargen.api.BuycraftAPI;
import net.samagames.rendlargen.payments.PaymentCheckerThread;
import net.samagames.rendlargen.payments.PaymentManager;
import net.samagames.rendlargen.utils.ChatColor;
import net.samagames.rendlargen.utils.JsonModMessage;
import net.samagames.rendlargen.utils.ModChannel;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.UUID;
import java.util.logging.Level;

/*
 * This file is part of RendLargen.
 *
 * RendLargen is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RendLargen is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RendLargen.  If not, see <http://www.gnu.org/licenses/>.
 */
public class RendLargen {
    private static RendLargen instance;

    private final SimpleDateFormat logDateFormat;
    private final BuycraftAPI buycraftAPI;
    private final GameServiceManager gameServiceManager;
    private final JedisPool jedisPool;
    private final PaymentManager paymentManager;
    private final boolean debug;
    private JsonObject configuration;

    public RendLargen() {
        instance = this;

        this.logDateFormat = new SimpleDateFormat("[dd/MM/yyyy HH:mm:ss] ");

        this.loadConfiguration();

        this.debug = this.configuration.get("debug").getAsBoolean();

        this.buycraftAPI = new BuycraftAPI(this, this.configuration.get("buycraft-api-key").getAsString());

        String sqlIp = this.configuration.get("sql-ip").getAsString();
        int sqlPort = this.configuration.get("sql-port").getAsInt();
        String sqlName = this.configuration.get("sql-name").getAsString();
        String sqlUsername = this.configuration.get("sql-user").getAsString();
        String sqlPassword = this.configuration.get("sql-pass").getAsString();

        this.gameServiceManager = new GameServiceManager(sqlIp, sqlUsername, sqlPassword, sqlName, sqlPort);

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(-1);
        config.setJmxEnabled(false);

        this.jedisPool = new JedisPool(config, this.configuration.get("redis-ip").getAsString(), this.configuration.get("redis-port").getAsInt(), 0, this.configuration.get("redis-password").getAsString());

        this.paymentManager = new PaymentManager(this);

        this.log(Level.INFO, "Starting payment checker thread...");
        new JsonModMessage("RendLargen", ModChannel.INFORMATION, ChatColor.GREEN, "Chargement initial des données de la boutique...").send();

        PaymentCheckerThread paymentChecker = new PaymentCheckerThread(this);
        paymentChecker.start();

        this.log(Level.INFO, "Waiting for payments...");
        new JsonModMessage("RendLargen", ModChannel.INFORMATION, ChatColor.GREEN, "Prêt !").send();

        Runtime.getRuntime().addShutdownHook(new Thread(this::disable));
        this.log(Level.INFO, "RendLargen enabled!");
    }

    public static RendLargen getInstance() {
        return instance;
    }

    public void disable() {
        new JsonModMessage("RendLargen", ModChannel.INFORMATION, ChatColor.GREEN, "Arrêt ! Attention, le pont entre la boutique et le serveur ne sera plus effectué.").send();
    }

    public void loadConfiguration() {
        this.log(Level.INFO, "Loading configuration...");

        File configurationFile = new File("config.json");

        if (!configurationFile.exists()) {
            this.log(Level.SEVERE, "config.json not found!");
            System.exit(1);
        }

        JsonObject configurationJson = new JsonObject();

        try {
            configurationJson = new JsonParser().parse(new FileReader(configurationFile)).getAsJsonObject();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        this.configuration = configurationJson;
    }

    public void updateConfiguration(long timestamp, UUID uuid) {
        this.configuration.remove("last-timestamp");
        this.configuration.addProperty("last-timestamp", timestamp);

        this.configuration.remove("last-uuid");
        this.configuration.addProperty("last-uuid", uuid.toString());

        try {
            File configurationFile = new File("config.json");
            configurationFile.delete();

            FileWriter writer = new FileWriter(configurationFile);
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(this.configuration));
            writer.close();
        } catch (IOException e) {
            this.log(Level.SEVERE, "Can't update configuration file!");
        }
    }

    public void log(Level level, String text) {
        if (level == Level.INFO)
            System.out.println(this.logDateFormat.format(System.currentTimeMillis()) + "[INFO] " + text);
        else if (level == Level.WARNING)
            System.out.println(this.logDateFormat.format(System.currentTimeMillis()) + "[WARNING] " + text);
        else if (level == Level.SEVERE)
            System.err.println(this.logDateFormat.format(System.currentTimeMillis()) + "[SEVERE] " + text);
    }

    public JsonObject getConfiguration() {
        return this.configuration;
    }

    public BuycraftAPI getBuycraftAPI() {
        return this.buycraftAPI;
    }

    public Jedis getJedis() {
        return this.jedisPool.getResource();
    }

    public PaymentManager getPaymentManager() {
        return this.paymentManager;
    }

    public boolean isDebug() {
        return this.debug;
    }

    public GameServiceManager getGameServiceManager() {
        return gameServiceManager;
    }
}
