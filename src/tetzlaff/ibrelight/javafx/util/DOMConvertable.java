package tetzlaff.ibrelight.javafx.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@FunctionalInterface
public interface DOMConvertable
{
    Element toDOMElement(Document document);
}
