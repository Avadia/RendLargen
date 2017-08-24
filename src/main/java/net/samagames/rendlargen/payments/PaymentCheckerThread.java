package net.samagames.rendlargen.payments;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.samagames.rendlargen.RendLargen;
import net.samagames.rendlargen.utils.UUIDUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
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
public class PaymentCheckerThread extends Thread
{
    private final RendLargen rendLargen;

    private long lastTimestamp;
    private UUID lastUUID;

    public PaymentCheckerThread(RendLargen rendLargen)
    {
        this.rendLargen = rendLargen;

        this.lastTimestamp = this.rendLargen.getConfiguration().get("last-timestamp").getAsLong();

        String lastUUIDConfiguration = this.rendLargen.getConfiguration().get("last-uuid").getAsString();

        if(!lastUUIDConfiguration.equals(""))
            this.lastUUID = UUID.fromString(this.rendLargen.getConfiguration().get("last-uuid").getAsString());
        else
            this.lastUUID = null;
    }

    @Override
    public void run()
    {
        while(true)
        {
            UUID lastPaymentsUUID = null;
            long lastPaymentsTimestamp = 0;

            JsonObject apiPaymentsResponse = null;

            try
            {
                apiPaymentsResponse = this.rendLargen.getBuycraftAPI().paymentsAction(120, null);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            if(apiPaymentsResponse == null)
            {
                this.rendLargen.log(Level.WARNING, "Can't ask Buycraft API, maybe a temporary error?");

                try
	            {
	                Thread.sleep(5000);
	            }
	            catch (InterruptedException e)
	            {
	                e.printStackTrace();
	            }
	            
                continue;
            }

            JsonArray lastPaymentsJson = apiPaymentsResponse.get("payload").getAsJsonArray();

            for(int i = 0; i < lastPaymentsJson.size(); i++)
            {
                JsonObject paymentJson = lastPaymentsJson.get(i).getAsJsonObject();

                long timestamp = paymentJson.get("time").getAsLong();
                UUID uuid = UUID.fromString(UUIDUtils.addDashesToUUID(paymentJson.get("uuid").getAsString()));

                if(this.lastTimestamp > timestamp || (this.lastTimestamp == timestamp && this.lastUUID != null && this.lastUUID.equals(uuid)))
                    continue;

                JsonArray packagesJson = paymentJson.get("packages").getAsJsonArray();
                ArrayList<Integer> packages = new ArrayList<>();

                for(int j = 0; j < packagesJson.size(); j++)
                    packages.add(packagesJson.get(j).getAsInt());

                this.rendLargen.log(Level.INFO, "New payment detected for player " + uuid.toString() + " (" + StringUtils.join(packages, ", ") + ")!");
                this.rendLargen.getPaymentManager().push(uuid);

                if(lastPaymentsTimestamp < timestamp)
                {
                    lastPaymentsTimestamp = timestamp;

                    if(lastPaymentsUUID == null || !lastPaymentsUUID.equals(uuid))
                        lastPaymentsUUID = uuid;
                }
            }

            if(lastPaymentsTimestamp != 0 && lastPaymentsUUID != null)
                this.updateConfiguration(lastPaymentsTimestamp, lastPaymentsUUID);

            try
            {
                Thread.sleep(5000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void updateConfiguration(long timestamp, UUID uuid)
    {
        this.lastTimestamp = timestamp;
        this.lastUUID = uuid;

        this.rendLargen.updateConfiguration(timestamp, uuid);
    }
}
