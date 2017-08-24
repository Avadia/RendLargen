package net.samagames.rendlargen.payments;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.samagames.persistanceapi.beans.players.PlayerBean;
import net.samagames.rendlargen.RendLargen;
import net.samagames.rendlargen.utils.UUIDUtils;
import redis.clients.jedis.Jedis;

import java.util.Calendar;
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
@SuppressWarnings("ALL")
public class PaymentApplicationTask implements Runnable
{
    private final RendLargen rendLargen;
    private final UUID uuid;

    public PaymentApplicationTask(RendLargen rendLargen, UUID uuid)
    {
        this.rendLargen = rendLargen;
        this.uuid = uuid;
    }

    @Override
    public void run()
    {
        if(this.rendLargen.isDebug())
        {
            if(!this.uuid.equals(UUID.fromString("29b2b527-1b59-45df-b7b0-d5ab20d8731a")))
            {
                this.rendLargen.log(Level.WARNING, "Production payment detected, aborting!");
                return;
            }
        }

        if(this.rendLargen.isDebug())
            this.rendLargen.log(Level.INFO, "> Starting payment application...");

        JsonObject apiPaymentsResponse = this.rendLargen.getBuycraftAPI().fetchPlayerCommands(new String[]{UUIDUtils.uuidToString(this.uuid)});

        if(apiPaymentsResponse == null)
        {
            this.rendLargen.log(Level.WARNING, "Can't ask Buycraft API, maybe a temporary error?");
            return;
        }

        JsonArray paymentsJson = apiPaymentsResponse.get("payload").getAsJsonObject().get("commands").getAsJsonArray();

        for(int i = 0; i < paymentsJson.size(); i++)
        {
            JsonObject paymentJson = paymentsJson.get(i).getAsJsonObject();

            int paymentId = paymentJson.get("id").getAsInt();
            String paymentCommand = paymentJson.get("command").getAsString();
            String[] paymentCommandSplitted = paymentCommand.split(" ");

            if(paymentCommandSplitted.length == 0)
            {
                this.rendLargen.log(Level.WARNING, "Received empty command for the payment of " + this.uuid.toString() + "! Aborting!");
            }
            else
            {
                if(this.rendLargen.isDebug())
                    this.rendLargen.log(Level.INFO, "> Command type is: " + paymentCommandSplitted[0]);

                if(paymentCommandSplitted[0].equals("rank"))
                {
                    int rank = Integer.valueOf(paymentCommandSplitted[1]);
                    Long timestamp = null;

                    if(paymentCommandSplitted.length > 2)
                    {
                        int month = Integer.valueOf(paymentCommandSplitted[2]);

                        Calendar now = Calendar.getInstance();
                        now.add(Calendar.MONTH, month);

                        timestamp = now.getTimeInMillis();
                    }

                    try {
                        long originalRank = 0;
                        PlayerBean player = rendLargen.getGameServiceManager().getPlayer(this.uuid, null);
                        originalRank = player.getGroupId();
                        player.setGroupId(rank);
                        rendLargen.getGameServiceManager().updatePlayer(player);

                        /*if (timestamp != null)
                        {
                            RestoreRank restoreRank = new RestoreRank(timestamp, uuid, originalRank);
                            rendLargen.getPaymentManager().getRankRestorator().offer(restoreRank);
                        }*/

                        Jedis jedis = this.rendLargen.getJedis();
                        jedis.publish("groupchange", "{\"playerUUID\":\"" + uuid + "\"}");
                        jedis.close();

                        this.success();
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rendLargen.log(Level.SEVERE, "> API returned a error");
                        this.rendLargen.log(Level.SEVERE, "> Rank of " + this.uuid.toString() + " have not been updated! (" + rank + (timestamp != null ? " for " + paymentCommandSplitted[2] + "months" : "") + ")");
                        continue;
                    }


                }
                else if (paymentCommandSplitted[0].equals("stars"))
                {
                    int starsToAdd = Integer.valueOf(paymentCommandSplitted[1]);

                    try {
                        PlayerBean player = rendLargen.getGameServiceManager().getPlayer(this.uuid, null);
                        player.setStars(player.getStars() + starsToAdd);
                        rendLargen.getGameServiceManager().updatePlayer(player);
                        Jedis jedis = rendLargen.getJedis();
                        String key = "playerdata:" + this.uuid;
                        String stars = jedis.hget(key, "Stars");
                        if (stars != null)
                            jedis.hset(key, "Stars","" + (Integer.valueOf(stars)+starsToAdd));
                        jedis.close();
                        this.success();
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.rendLargen.log(Level.SEVERE, "> API returned a error");
                        this.rendLargen.log(Level.SEVERE, "> Stars of " + this.uuid.toString() + " have not been updated! ( need to add " + starsToAdd + ")!");
                        continue;
                    }
                }
                else if (paymentCommandSplitted[0].equals("chargeback"))
                {
                    //Disabled for now
                    /*RestAPI.getInstance().sendRequest("player/ban", new Request().addProperty("reason", "Rejet de paiement sur la boutique").addProperty("playerUUID", this.uuid.toString()).addProperty("punisherUUID", "29b2b527-1b59-45df-b7b0-d5ab20d8731a").addProperty("expiration", 0), StatusResponse.class, "POST");
                    new JsonModMessage("RendLargen", ModChannel.SANCTION, ChatColor.GREEN, "Le joueur '" + this.uuid.toString() + "' a été banni pour rejet de paiement sur la boutique !");

                    Jedis jedis = this.rendLargen.getJedis();
                    jedis.publish("apiexec.kick", this.uuid.toString() + " {\"text\":\"" + "Vous êtes banni : Rejet de paiement sur la boutique" + "\"}");
                    jedis.close();*/
                }
                else
                {
                    this.rendLargen.log(Level.WARNING, "> Unknown payment command for player " + this.uuid.toString() + "!");
                }
            }

            this.rendLargen.getBuycraftAPI().commandsDeleteAction(new int[]{paymentId});
        }

        this.rendLargen.log(Level.INFO, "Payment applied for player " + this.uuid.toString() + "!");
    }

    private void success()
    {
        Jedis jedis = this.rendLargen.getJedis();
        jedis.publish("shopsuccessful", this.uuid.toString());
        jedis.close();
    }
}
