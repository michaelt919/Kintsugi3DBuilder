/*
 * Copyright (c) 2019 - 2024 Seth Berrier, Michael Tetzlaff, Jacob Buelow, Luke Denney, Ian Anderson, Zoe Cuthrell, Blane Suess, Isaac Tesch, Nathaniel Willius
 * Copyright (c) 2019 The Regents of the University of Minnesota
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package kintsugi3d.builder.util;

import kintsugi3d.builder.app.ApplicationFolders;
import kintsugi3d.builder.app.Kintsugi3DBuilder;
import kintsugi3d.builder.app.OperatingSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Launch4jConfiguration
{
    private static final Logger log = LoggerFactory.getLogger(Launch4jConfiguration.class);

    private static final Path l4jIniFile = Paths.get(ApplicationFolders.APP_FOLDER_NAME + ".l4j.ini");

    private boolean enableMaxMemory = false;
    private int maxMemoryMb = 4096;

    private Launch4jConfiguration() {}

    public void write() throws IOException
    {
        log.info("Writing launch4j configuration file to {}", l4jIniFile.toAbsolutePath());
        BufferedWriter writer = Files.newBufferedWriter(l4jIniFile);
        writeTo(writer);
    }

    public void writeAsSuperuser() throws IOException, IllegalStateException
    {
        log.info("Writing launch4j configuration file AS ADMINISTRATOR to {}", l4jIniFile.toAbsolutePath());
        if (ApplicationFolders.getCurrentOS() != OperatingSystem.WINDOWS)
        {
            throw new IllegalStateException("Writing as superuser not supported for OS");
        }

        StringWriter writer = new StringWriter();
        writeTo(writer);
        String fileContent = writer.toString();

        String subcommand = String.format("\\\"New-Item '%s' -ItemType File -Force -Value '%s'\\\"",
                l4jIniFile.toFile().getAbsolutePath(), fileContent);
        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", "Start-Process", "PowerShell",
                "-verb", "runas", "-ArgumentList", subcommand);
        pb.start();
    }

    private void writeTo(Writer writer) throws IOException
    {
        if (enableMaxMemory)
        {
            writer.write(String.format("-Xmx%dm\n", maxMemoryMb));
        }

        writer.close();
    }

    public static Launch4jConfiguration read() throws IOException
    {
        if (! Files.exists(l4jIniFile))
        {
            return new Launch4jConfiguration();
        }

        Launch4jConfiguration config = new Launch4jConfiguration();
        List<String> lines = Files.readAllLines(l4jIniFile);

        for (String line : lines)
        {
            // Parse maximum memory
            if (line.startsWith("-Xmx"))
            {
                config.enableMaxMemory = true;
                config.maxMemoryMb = parseMegabytesFromHeap(line);
            }
        }

        return config;
    }

    public static Launch4jConfiguration empty()
    {
        return new Launch4jConfiguration();
    }

    private static int parseMegabytesFromHeap(String heapArg)
    {
        Pattern valPattern = Pattern.compile("\\d+");
        Matcher valMatcher = valPattern.matcher(heapArg);

        Integer value;
        if (valMatcher.find())
        {
            value = Integer.parseInt(valMatcher.group());
        }
        else
        {
            return 0;
        }

        Pattern suffixPattern = Pattern.compile("\\D+$");
        Matcher suffixMatcher = suffixPattern.matcher(heapArg);

        String suffix;
        if (suffixMatcher.find())
        {
            suffix = suffixMatcher.group();
        }
        else
        {
            return 0;
        }

        float multiplier = 1;
        if (suffix.toLowerCase().charAt(0) == 'g')
        {
            multiplier = 1024;
        }

        return (int)((float)value * multiplier);
    }

    public int getMaxMemoryMb()
    {
        return maxMemoryMb;
    }

    public void setMaxMemoryMb(int maxMemoryMb)
    {
        this.maxMemoryMb = maxMemoryMb;
    }

    public boolean isEnableMaxMemory()
    {
        return enableMaxMemory;
    }

    public void setEnableMaxMemory(boolean enableMaxMemory)
    {
        this.enableMaxMemory = enableMaxMemory;
    }
}
