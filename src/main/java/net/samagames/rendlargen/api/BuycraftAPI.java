package net.samagames.rendlargen.api;

import com.google.gson.*;
import net.samagames.rendlargen.RendLargen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
public class BuycraftAPI
{
    private final RendLargen rendLargen;
    private final String apiKey;
    private final Gson gson;

    public BuycraftAPI(RendLargen rendLargen, String apiKey)
    {
        this.rendLargen = rendLargen;
        this.apiKey = apiKey;

        this.gson = new Gson();
    }

    public JsonObject categoriesAction()
    {
        HashMap<String, String> apiCallParams = new HashMap<>();
        apiCallParams.put("action", "categories");

        return call(apiCallParams);
    }

    public JsonObject urlAction(String url)
    {
        HashMap<String, String> apiCallParams = new HashMap<>();

        apiCallParams.put("action", "url");
        apiCallParams.put("url", url);

        return call(apiCallParams);
    }

    public JsonObject packagesAction()
    {
        HashMap<String, String> apiCallParams = new HashMap<>();
        apiCallParams.put("action", "packages");

        return call(apiCallParams);
    }

    public JsonObject paymentsAction(int limit, String username)
    {
        HashMap<String, String> apiCallParams = new HashMap<>();
        apiCallParams.put("action", "payments");
        apiCallParams.put("limit", String.valueOf(limit));

        if(username != null)
            apiCallParams.put("ign", username);

        return call(apiCallParams);
    }

    public JsonObject fetchPendingPlayers()
    {
        HashMap<String, String> apiCallParams = new HashMap<>();
        apiCallParams.put("action", "pendingUsers");
        apiCallParams.put("userType", "uuid");

        return call(apiCallParams);
    }

    public JsonObject fetchPlayerCommands(String[] players)
    {
        HashMap<String, String> apiCallParams = new HashMap<>();
        apiCallParams.put("action", "commands");
        apiCallParams.put("do", "lookup");

        JsonArray usersJson = new JsonArray();

        for(String player : players)
            usersJson.add(new JsonPrimitive(player));

        apiCallParams.put("users", this.gson.toJson(usersJson));
        apiCallParams.put("userType", "uuid");
        apiCallParams.put("offlineCommands", String.valueOf(true));
        apiCallParams.put("offlineCommandLimit", "150");

        return call(apiCallParams);
    }

    public void commandsDeleteAction(int[] commands)
    {
        HashMap<String, String> apiCallParams = new HashMap<>();

        apiCallParams.put("action", "commands");
        apiCallParams.put("do", "removeId");

        JsonArray commandsJson = new JsonArray();

        for(int command : commands)
            commandsJson.add(new JsonPrimitive(command));

        apiCallParams.put("commands", this.gson.toJson(commandsJson));

        call(apiCallParams);
    }

    private JsonObject call(HashMap<String, String> apiCallParams)
    {
        apiCallParams.put("secret", this.apiKey);

        String url = "https://api.buycraft.net/v4" + this.generateUrlQueryString(apiCallParams);
        String response = this.makeRequest(url);

        try
        {
            if (response != null)
                return new JsonParser().parse(response).getAsJsonObject();
        }
        catch (Exception e)
        {
            this.rendLargen.log(Level.SEVERE, "Exception on parsing Buycraft's json response!");
        }

        return null;
    }

    public String makeRequest(String url)
    {
        try
        {
            URL urll = new URL(url);

            HttpURLConnection httpConnection = (HttpURLConnection) urll.openConnection();

            httpConnection.setRequestMethod("GET");
            httpConnection.setConnectTimeout(15000);
            httpConnection.setReadTimeout(15000);
            httpConnection.setInstanceFollowRedirects(false);
            httpConnection.setAllowUserInteraction(false);

            BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));

            String content = "";
            String inputLine;

            while ((inputLine = in.readLine()) != null)
                content = content + inputLine;

            in.close();

            return content;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    private String generateUrlQueryString(HashMap<String, String> arguments)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("?");

        for (Map.Entry<String, String> entry : arguments.entrySet())
        {
            if (sb.length() > 1)
                sb.append("&");

            sb.append(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }

        return sb.toString();
    }
}
