package net.samagames.rendlargen.utils;

import java.util.UUID;

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
public class UUIDUtils
{
    public static String addDashesToUUID(String s)
    {
        StringBuilder b = new StringBuilder();
        return b.append(s, 0, 8).append('-').append(s, 8, 12).append('-').append(s, 12, 16).append('-').append(s, 16, 20).append('-').append(s, 20, 32).toString();
    }

    public static String uuidToString(UUID uuid)
    {
        return uuid.toString().replaceAll("-", "");
    }
}
