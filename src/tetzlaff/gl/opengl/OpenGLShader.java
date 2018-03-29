package tetzlaff.gl.opengl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Pattern;

import tetzlaff.gl.core.Shader;
import tetzlaff.gl.exceptions.IllegalShaderDefineException;
import tetzlaff.gl.exceptions.ShaderCompileFailureException;
import tetzlaff.gl.exceptions.ShaderPreprocessingFailureException;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

class OpenGLShader implements Shader<OpenGLContext>
{
    private static final Pattern QUOTATION_MARK_PATTERN = Pattern.compile("['\"]");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    protected final OpenGLContext context;

    private int shaderId;

    OpenGLShader(OpenGLContext context, int shaderType, File file, Map<String, Object> defines) throws FileNotFoundException
    {
        this.context = context;

        StringBuilder sb = new StringBuilder(1024);

        loadSource(file, sb, defines);
        
        try
        {
            this.init(shaderType, sb.toString());
        }
        catch (ShaderCompileFailureException e)
        {
            throw new ShaderCompileFailureException(file.getAbsolutePath() + " failed to compile.", e);
        }
    }

    private static void validateDefine(String key, Object value)
    {
        String valueString = value.toString();
        int parenthesesDepth = 0;
        for (int i = 0; i < valueString.length(); i++)
        {
            char nextChar = valueString.charAt(i);
            if (nextChar == '(')
            {
                parenthesesDepth++;
            }
            else if (nextChar == ')')
            {
                parenthesesDepth--;

                if (parenthesesDepth < 0)
                {
                    throw new IllegalShaderDefineException("Unbalanced parentheses in the definition for " + key);
                }
            }
        }
    }

    private static void handleLineContinuation(Scanner scanner, String finishedLine)
    {
        String currentLine = finishedLine;
        while (currentLine.endsWith("\\") && scanner.hasNextLine())
        {
            currentLine = scanner.nextLine();
        }

        if (currentLine.endsWith("\\"))
        {
            while(scanner.hasNext())
            {
                scanner.next();
            }
        }
    }

    private static void loadSource(File file, StringBuilder sb, Map<String, Object> defines) throws FileNotFoundException
    {
        int lineCounter = 1;
        boolean definesAdded = defines.isEmpty();

        try(Scanner scanner = new Scanner(file))
        {
            while (scanner.hasNextLine())
            {
                String nextLine = scanner.nextLine();

                String trimmedLine = nextLine.trim();

                if (!definesAdded && !trimmedLine.isEmpty() && !trimmedLine.startsWith("#version") && !trimmedLine.startsWith("#extension"))
                {
                    for (Entry<String, Object> define : defines.entrySet())
                    {
                        String key = define.getKey();
                        Object value = define.getValue();

                        validateDefine(key, value);

                        sb.append("#define ");
                        sb.append(key);
                        sb.append(" ( ");

                        if (value instanceof Boolean)
                        {
                            if ((Boolean)value)
                            {
                                sb.append(1);
                            }
                            else
                            {
                                sb.append(0);
                            }
                        }
                        else
                        {
                            sb.append(value);
                        }

                        sb.append(" )");
                        sb.append(System.lineSeparator());
                    }

                    sb.append("#line ");
                    sb.append(lineCounter);
                    sb.append(" 0");
                    sb.append(System.lineSeparator());

                    definesAdded = true;
                }

                if (trimmedLine.startsWith("#include"))
                {
                    String[] parts = WHITESPACE_PATTERN.split(nextLine);
                    File includeFile;
                    if (parts.length < 2)
                    {
                        // This won't work of course, but it's a reasonable way to generate an exception in a manner consistent with a non-existent file.
                        includeFile = file.getParentFile();
                    }
                    else if (parts[1].charAt(0) == '<' && parts[1].charAt(parts[1].length() - 1) == '>')
                    {
                        includeFile = new File(parts[1].substring(1, parts[1].length() - 1));
                    }
                    else
                    {
                        String filename = QUOTATION_MARK_PATTERN.matcher(parts[1]).replaceAll(""); // Remove single or double quotes around filename
                        includeFile = new File(file.getParentFile(), filename);
                    }

                    loadSource(includeFile, sb, Collections.emptyMap());
                }
                else
                {
                    sb.append(nextLine);
                    sb.append(System.lineSeparator());
                }

                lineCounter++;
            }

            // Read remaining characters in case the file is not newline-terminated.
            //noinspection HardcodedFileSeparator
            scanner.useDelimiter("\\Z");
            if (scanner.hasNext())
            {
                sb.append(scanner.next());
            }
        }
        catch (IllegalShaderDefineException e)
        {
            throw e;
        }
        catch(RuntimeException e)
        {
            throw new ShaderPreprocessingFailureException("An exception occurred while processing line " + lineCounter + " of the following shader file: " + file, e);
        }
    }

    OpenGLShader(OpenGLContext context, int shaderType, CharSequence source)
    {
        this.context = context;
        this.init(shaderType, source);
    }

    @Override
    public OpenGLContext getContext()
    {
        return this.context;
    }

    private void init(int shaderType, CharSequence source)
    {
        shaderId = glCreateShader(shaderType);
        OpenGLContext.errorCheck();
        glShaderSource(shaderId, source);
        OpenGLContext.errorCheck();
        glCompileShader(shaderId);
        OpenGLContext.errorCheck();
        int compiled = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        OpenGLContext.errorCheck();
        if (compiled == GL_FALSE)
        {
            throw new ShaderCompileFailureException(glGetShaderInfoLog(shaderId));
        }
    }

    int getShaderId()
    {
        return shaderId;
    }

    @Override
    public void close()
    {
        glDeleteShader(shaderId);
        OpenGLContext.errorCheck();
    }
}
