/*
 * Copyright (c) 2018
 * The Regents of the University of Minnesota
 * All rights reserved.
 *
 * Licensed under GPLv3
 * ( http://www.gnu.org/licenses/gpl-3.0.html )
 *
 * This code is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 */

package umn.reflectancefit;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import javax.swing.*;

import umn.gl.exceptions.GLOutOfMemoryException;
import umn.gl.glfw.WindowFactory;
import umn.gl.glfw.WindowImpl;
import umn.gl.opengl.OpenGLContext;
import umn.gl.window.Window;

/**
 * The main class for the reflectance parameter fitting program.
 * Creates the user interface and provides a handler for the execute button that creates an OpenGL graphics context and passes it to an MainExecution
 * object that uses the graphics context to run the reflectance parameter estimation algorithm.
 */
public final class ReflectanceFitProgram
{
    private static final boolean DEBUG = false;

    private ReflectanceFitProgram()
    {
    }

    private static void runProgram()
    {
        UserInterface gui = new UserInterface();

        gui.addExecuteButtonActionListener(e ->
        {
            if (gui.getCameraFile() == null)
            {
                JOptionPane.showMessageDialog(gui, "No camera file selected.", "Please select a camera file.", JOptionPane.ERROR_MESSAGE);
            }
            else if (gui.getModelFile() == null)
            {
                JOptionPane.showMessageDialog(gui, "No model file selected.", "Please select a model file.", JOptionPane.ERROR_MESSAGE);
            }
            else if (gui.getImageDirectory() == null)
            {
                JOptionPane.showMessageDialog(gui, "No undistorted photo directory selected.", "Please select an undistorted photo directory.", JOptionPane.ERROR_MESSAGE);
            }
            else if (gui.getOutputDirectory() == null)
            {
                JOptionPane.showMessageDialog(gui, "No destination directory selected.", "Please select a destination directory for models and textures.", JOptionPane.ERROR_MESSAGE);
            }
            else if (gui.getParameters().isImageRescalingEnabled() && gui.getRescaleDirectory() == null)
            {
                JOptionPane.showMessageDialog(gui, "No resized photo destination selected.", "Please select a destination directory for resized photos, or disable photo resizing.", JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                try(Window<OpenGLContext> window = WindowFactory.buildOpenGLWindow("Texture Generation", 800, 800).create())
                {
                    OpenGLContext context = window.getContext();

                    new MainExecution<>(context, gui.getCameraFile(), gui.getModelFile(), gui.getImageDirectory(), gui.getMaskDirectory(),
                        gui.getRescaleDirectory(), gui.getOutputDirectory(), gui.getParameters())
                            .execute();
                }
                catch (HeadlessException ex)
                {
                    ex.printStackTrace();
                }
                catch (IOException ex)
                {
                    System.gc(); // Suggest garbage collection.
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(gui, ex.getMessage(), "Error reading file (" + ex.getClass().getName() + ')', JOptionPane.ERROR_MESSAGE);
                }
                catch (GLOutOfMemoryException ex)
                {
                    System.gc(); // Suggest garbage collection.
                    ex.printStackTrace();
                    ByteArrayOutputStream stackTraceStream = new ByteArrayOutputStream();
                    PrintStream stackTracePrintStream = new PrintStream(stackTraceStream);
                    ex.printStackTrace(stackTracePrintStream);
                    stackTracePrintStream.flush();
                    stackTracePrintStream.close();
                    JOptionPane.showMessageDialog(gui, "You've run out of graphics memory.  " +
                        "Reduce the number of photos, or try using either smaller photos or pre-projected photos to reduce the amount of graphics memory usage.  Stack Trace: " +
                        stackTraceStream, "Out of graphics memory", JOptionPane.ERROR_MESSAGE);
                }
                catch (RuntimeException ex)
                {
                    System.gc(); // Suggest garbage collection.
                    ex.printStackTrace();
                    ByteArrayOutputStream stackTraceStream = new ByteArrayOutputStream();
                    PrintStream stackTracePrintStream = new PrintStream(stackTraceStream);
                    ex.printStackTrace(stackTracePrintStream);
                    stackTracePrintStream.flush();
                    stackTracePrintStream.close();
                    JOptionPane.showMessageDialog(gui, stackTraceStream.toString(), ex.getClass().getName(), JOptionPane.ERROR_MESSAGE);
                }
                finally
                {
                    WindowImpl.closeAllWindows();
                }
                System.out.println("Process terminated with no errors.");
            }
        });

        gui.setVisible(true);
    }

    public static void main(String... args) throws FileNotFoundException
    {
        if (!DEBUG)
        {
            PrintStream out = new PrintStream("out.log");
            PrintStream err = new PrintStream("err.log");
            System.setOut(out);
            System.setErr(err);
        }

        runProgram();
    }
}
