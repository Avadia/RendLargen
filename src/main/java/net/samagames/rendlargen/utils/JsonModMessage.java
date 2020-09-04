package net.samagames.rendlargen.utils;

import com.google.gson.Gson;
import net.samagames.rendlargen.RendLargen;
import redis.clients.jedis.Jedis;

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
public class JsonModMessage {
    protected String sender;
    protected ModChannel modChannel;
    protected ChatColor senderPrefix;
    protected String message;

    public JsonModMessage(String sender, ModChannel modChannel, ChatColor senderPrefix, String message) {
        this.sender = sender;
        this.modChannel = modChannel;
        this.senderPrefix = senderPrefix;
        this.message = message;
    }

    public void send() {
        Jedis jedis = RendLargen.getInstance().getJedis();
        jedis.publish("moderationchan", new Gson().toJson(this));
        jedis.close();
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public ChatColor getSenderPrefix() {
        return senderPrefix;
    }

    public void setSenderPrefix(ChatColor senderPrefix) {
        this.senderPrefix = senderPrefix;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}