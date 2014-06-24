package map;
import java.awt.Canvas;

//from NewtCanvasAWT
import java.applet.Applet;
import java.awt.AWTKeyStroke;
import java.awt.Canvas;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.KeyboardFocusManager;
import java.awt.geom.NoninvertibleTransformException;
import java.beans.Beans;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.media.nativewindow.NativeWindow;
import javax.media.nativewindow.OffscreenLayerOption;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.swing.MenuSelectionManager;

import jogamp.nativewindow.awt.AWTMisc;
import jogamp.nativewindow.jawt.JAWTUtil;
import jogamp.newt.Debug;
import jogamp.newt.WindowImpl;
import jogamp.newt.awt.NewtFactoryAWT;
import jogamp.newt.awt.event.AWTParentWindowAdapter;
import jogamp.newt.driver.DriverClearFocus;
import jogamp.opengl.awt.AWTTilePainter;

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.nativewindow.awt.AWTPrintLifecycle;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.newt.Display;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.awt.AWTAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.TileRenderer;

//from GLCanvas
import java.beans.Beans;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.media.nativewindow.AbstractGraphicsConfiguration;
import javax.media.nativewindow.OffscreenLayerOption;
import javax.media.nativewindow.ScalableSurface;
import javax.media.nativewindow.VisualIDHolder;
import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.nativewindow.AbstractGraphicsScreen;
import javax.media.nativewindow.GraphicsConfigurationFactory;
import javax.media.nativewindow.NativeSurface;
import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.GL;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;
import javax.media.opengl.GLRunnable;
import javax.media.opengl.GLSharedContextSetter;
import javax.media.opengl.Threading;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
import com.jogamp.nativewindow.awt.AWTGraphicsConfiguration;
import com.jogamp.nativewindow.awt.AWTGraphicsDevice;
import com.jogamp.nativewindow.awt.AWTGraphicsScreen;
import com.jogamp.nativewindow.awt.AWTPrintLifecycle;
import com.jogamp.nativewindow.awt.AWTWindowClosingProtocol;
import com.jogamp.nativewindow.awt.JAWTWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.TileRenderer;

import jogamp.nativewindow.SurfaceScaleUtils;
import jogamp.opengl.Debug;
import jogamp.opengl.GLContextImpl;
import jogamp.opengl.GLDrawableHelper;
import jogamp.opengl.GLDrawableImpl;
import jogamp.opengl.awt.AWTTilePainter;

//from WorldWindowGLCanvas
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.*;

import javax.media.opengl.*;
import javax.media.opengl.awt.GLCanvas;
import java.awt.*;
import java.beans.*;
import java.util.*;


public class myCanvas extends java.awt.Canvas{

}
