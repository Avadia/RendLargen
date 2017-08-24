package net.samagames.rendlargen.payments;

import net.samagames.rendlargen.RendLargen;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
public class PaymentManager
{
    private final RendLargen rendLargen;
    private final ExecutorService executor;

    private PriorityQueue<RestoreRank> rankRestorator;

    public PaymentManager(RendLargen rendLargen)
    {
        this.rendLargen = rendLargen;
        this.executor = Executors.newFixedThreadPool(1);

        rankRestorator = new PriorityQueue<>(new Comparator<RestoreRank>() {
            @Override
            public int compare(RestoreRank o1, RestoreRank o2) {
                return Long.compare(o1.getTimeToExec(), o2.getTimeToExec());
            }
        });
    }

    public void push(UUID uuid)
    {
        this.executor.submit(new PaymentApplicationTask(this.rendLargen, uuid));
    }

    public PriorityQueue<RestoreRank> getRankRestorator() {
        return rankRestorator;
    }
}
