/*
 * http://code.google.com/p/ametro/
 * Transport map viewer for Android platform
 * Copyright (C) 2009-2010 Roman.Golovanov@gmail.com and other
 * respective project committers (see project home page)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.ametro.util.csv;

import junit.framework.TestCase;
import org.ametro.util.DateUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.text.ParseException;

/**
 * @author Vlad Vinichenko (akerigan@gmail.com)
 *         Date: 08.02.2010
 *         Time: 23:41:49
 */
public class CsvWriteTest {

    public static void main(String[] args) throws IOException, ParseException {
        CsvWriter csvWriter = new CsvWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
        // first record
        csvWriter.newRecord();
        csvWriter.writeInt(1);
        csvWriter.writeString("test1");
        csvWriter.writeDouble(1.1);
        csvWriter.writeBoolean(true);
        csvWriter.writeDate(DateUtil.parseDate("01.01.2010"));

        // second record
        csvWriter.newRecord();
        csvWriter.writeInt(2);
        csvWriter.writeString("test2");
        csvWriter.writeDouble(2.2);
        csvWriter.writeBoolean(false);
        csvWriter.writeDate(DateUtil.parseDate("02.02.2010"));

        csvWriter.flush();

    }

}
