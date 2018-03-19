package tetzlaff.gl.exceptions;

public class IllegalShaderDefineException extends GLException
{
    private static final long serialVersionUID = 1905386674496150406L;

    public IllegalShaderDefineException(String reason)
    {
        super(reason);
    }
}
