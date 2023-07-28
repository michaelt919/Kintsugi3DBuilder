package kintsugi3d.builder.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Launch4jConfiguration
{
    private static final Path l4jIniFile = Paths.get("IBRelight.l4j.ini");

    private boolean enableMaxMemory = false;
    private int maxMemoryMb = 4096;

    private Launch4jConfiguration() {}

    public void write() throws IOException
    {
        BufferedWriter writer = Files.newBufferedWriter(l4jIniFile);

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
