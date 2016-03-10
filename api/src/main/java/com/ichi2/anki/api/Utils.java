/***************************************************************************************
 *                                                                                      *
 * Copyright (c) 2016 Timothy Rae <perceptualchaos2@gmail.com>                          *
 *                                                                                      *
 * This program is free software; you can redistribute it and/or modify it under        *
 * the terms of the GNU Lesser General Public License as published by the Free Software *
 * Foundation; either version 3 of the License, or (at your option) any later           *
 * version.                                                                             *
 *                                                                                      *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY      *
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A      *
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.             *
 *                                                                                      *
 * You should have received a copy of the GNU Lesser General Public License along with  *
 * this program.  If not, see <http://www.gnu.org/licenses/>.                           *
 ****************************************************************************************/

package com.ichi2.anki.api;

import android.text.Html;
import android.text.TextUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilities class for the API
 */
class Utils {
    // Regex pattern used in removing tags from text before checksum
    private static final Pattern stylePattern = Pattern.compile("(?s)<style.*?>.*?</style>");
    private static final Pattern scriptPattern = Pattern.compile("(?s)<script.*?>.*?</script>");
    private static final Pattern tagPattern = Pattern.compile("<.*?>");
    private static final Pattern imgPattern = Pattern.compile("<img src=[\\\"']?([^\\\"'>]+)[\\\"']? ?/?>");
    private static final Pattern htmlEntitiesPattern = Pattern.compile("&#?\\w+;");


    static String joinFields(String[] list) {
        // note: since list is very unlikely to be length <2, any optimizations for those lengths are irrelevant
        return TextUtils.join("\u001f", list);
    }


    static String[] splitFields(String fields) {
        return fields.split("\\x1f", -1);
    }

    static String joinTags(Set<String> tags) {
        for (String t : tags) {
            t.replaceAll(" ", "_");
        }
        return TextUtils.join(" ", tags);
    }

    static String[] splitTags(String tags) {
        return tags.trim().split("\\s+");
    }

    static Long fieldChecksum(String data) {
        data = stripHTMLMedia(data);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] digest = md.digest(data.getBytes("UTF-8"));
            BigInteger biginteger = new BigInteger(1, digest);
            String result = biginteger.toString(16);
            return Long.valueOf(result.substring(0, 8), 16);
        } catch (Exception e) {
            // This is guaranteed to never happen
            throw new IllegalStateException("Error making field checksum with SHA1 algorithm and UTF-8 encoding", e);
        }
    }

    /**
     * Strip HTML but keep media filenames
     */
    private static String stripHTMLMedia(String s) {
        Matcher imgMatcher = imgPattern.matcher(s);
        return stripHTML(imgMatcher.replaceAll(" $1 "));
    }

    private static String stripHTML(String s) {
        Matcher htmlMatcher = stylePattern.matcher(s);
        s = htmlMatcher.replaceAll("");
        htmlMatcher = scriptPattern.matcher(s);
        s = htmlMatcher.replaceAll("");
        htmlMatcher = tagPattern.matcher(s);
        s = htmlMatcher.replaceAll("");
        return entsToTxt(s);
    }

    /**
     * Takes a string and replaces all the HTML symbols in it with their unescaped representation.
     * This should only affect substrings of the form &something; and not tags.
     * Internet rumour says that Html.fromHtml() doesn't cover all cases, but it doesn't get less
     * vague than that.
     * @param html The HTML escaped text
     * @return The text with its HTML entities unescaped.
     */
    private static String entsToTxt(String html) {
        // entitydefs defines nbsp as \xa0 instead of a standard space, so we
        // replace it first
        html = html.replace("&nbsp;", " ");
        Matcher htmlEntities = htmlEntitiesPattern.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (htmlEntities.find()) {
            htmlEntities.appendReplacement(sb, Html.fromHtml(htmlEntities.group()).toString());
        }
        htmlEntities.appendTail(sb);
        return sb.toString();
    }

    /** Given a list of integers, return a string '(int1,int2,...)'. */
    static <T> String ids2str(List<T> ids) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("(");
        boolean isNotFirst = false;
        for (T id : ids) {
            if (isNotFirst) {
                sb.append(", ");
            } else {
                isNotFirst = true;
            }
            sb.append(id);
        }
        sb.append(")");
        return sb.toString();
    }

}
