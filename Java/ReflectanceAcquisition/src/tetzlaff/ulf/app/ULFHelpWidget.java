package tetzlaff.ulf.app;

import com.trolltech.qt.core.QUrl;
import com.trolltech.qt.gui.QTextBrowser;
import com.trolltech.qt.gui.QWidget;
import com.trolltech.qt.help.QHelpEngine;

public class ULFHelpWidget extends QTextBrowser {

	private QHelpEngine helpEngine;
	
	public ULFHelpWidget(QHelpEngine engine, QWidget parent)
	{
		super(parent);
		this.helpEngine = engine;
	}
	
	@Override
    public Object loadResource(int type, QUrl name)
    {
		// Intercept any resources that come from the help engine
    	if (name.scheme().equalsIgnoreCase("qthelp"))
    	{
            return helpEngine.fileData(name);
    	}
        else
        {
            return super.loadResource(type, name);
        }
    }

}
