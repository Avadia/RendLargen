package net.samagames.rendlargen.utils;

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
public enum ModChannel {
    DISCUSSION(ChatColor.DARK_AQUA, "Discussion"),
    SANCTION(ChatColor.RED, "Sanction"),
    REPORT(ChatColor.GOLD, "Signalement"),
    INFORMATION(ChatColor.GREEN, "Information");

    private final String color;
    private final String name;

    ModChannel(ChatColor color, String name) {
        this.color = color.name();
        this.name = name;
    }

    public ChatColor getColor() {
        return ChatColor.valueOf(this.color);
    }

    public String getName() {
        return this.name;
    }
}
