package org.geworkbench.engine.config;

import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.JMenuItem;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geworkbench.engine.config.rules.MalformedMenuItemException;
import org.geworkbench.engine.config.rules.NotMenuListenerException;
import org.geworkbench.engine.config.rules.NotVisualPluginException;
import org.geworkbench.engine.config.rules.PluginObject;
import org.geworkbench.engine.management.ComponentRegistry;
import org.geworkbench.engine.management.ComponentResource;

/**
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: First Genetic Trust, Inc.</p>
 *
 * @author First Genetic Trust, Inc.
 * @version $Id$
 */

/**
 * The <code>PluginDescriptor</code> class stores references to the plugin
 * components comprising the application. These components are initialized as
 * a result of processing the <code>&lt;plugin&gt;</code> rules in the
 * application's configuration file.
 */
public class PluginDescriptor extends IdentifiableImpl implements Comparable<PluginDescriptor> {
    // ---------------------------------------------------------------------------
    // --------------- Instance and static variables
    // ---------------------------------------------------------------------------

    static Log log = LogFactory.getLog(PluginDescriptor.class);

    /**
     * Instance of a plugin component.
     */
    private Object plugin = null;
    /**
     * The keys of this map are <code>JMenuItem</code>s whose selection results
     * in a notification to the plugin ONLY if the plugin has the focus (this
     * plug in must be one implementing the <code>VisualPlugin</code> interface.
     * Every key is mapped to a
     * <code>Vector</code> containing all <code>ActionListener</code>s that
     * the plugin has registered with the corresponding menu item.
     */
    private HashMap<JMenuItem, Vector<ActionListener>> onFocusMenuItems = new HashMap<JMenuItem, Vector<ActionListener>>();
    /**
     * The keys of this map are <code>JMenuItem</code>s whose selection results
     * in a notification being sent to the plugin REGARDLESS of the plugin having
     * the focus or not (the plugin is not required in this case to implement the
     * <code>VisualPlugin</code> interface. Every key is mapped to a
     * <code>Vector</code> containing all <code>ActionListener</code>s that
     * the plugin has registered with the corresponding menu item.
     */
    private HashMap<JMenuItem, Vector<ActionListener>> alwaysMenuItems = new HashMap<JMenuItem, Vector<ActionListener>>();
    /**
     * the information about the menuitems.
     */
    // TODO menuItemInfos has no effect
    private ArrayList<HashMap<String, String>> menuItemInfos = new ArrayList<HashMap<String, String>>();

    /**
     * Stores the component for each module method.
     */
    private Map<String, Object> modules = new HashMap<String, Object>();

    /**
     * Stores the configuration module mappings (name -> id).
     */
    private Map<String, String> moduleMappings = new HashMap<String, String>();

    /**
     * Stores the visual location directive
     */
    // TODO visualLocation has no effect
    private String visualLocation;

    /**
     * Order in which this component was loaded, and which it prefers to be displayed relative to others.
     */
    private int preferredOrder;

    /**
     * The class of this plugin descriptor.
     */
    private Class<?> pluginClass;

    /**
     * Set of subscription types to ignore.
     */
    private Set<Class<?>> subscriptionIgnoreSet = new HashSet<Class<?>>();

    private ClassLoader loader;

    private ComponentResource resource;

    // ---------------------------------------------------------------------------
    // --------------- Constructors
    // ---------------------------------------------------------------------------
    /**
     * Instantiates a descriptor for a plugin component described by a class file
     * and assignes to it the
     * designated id and name.
     *
     * @param className A string containing the fully qualified class name.
     * @param someID          Assigned id.
     * @param someName        Assigned name.
     */
    public PluginDescriptor(String className, String someID, String someName, String resourceName, int preferredOrder) {
        super(someID, someName);
        this.preferredOrder = preferredOrder;

        resource = null;
        if (resourceName != null) {
            resource = ComponentRegistry.getRegistry().getComponentResourceByName(resourceName);
            if (resource == null) {
                System.out.println("Warning: Resource '" + resourceName + "' for component '" + someName + "' not found.");
            }
        }
        ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (resource == null) {
                pluginClass = Class.forName(className);
                loader = pluginClass.getClassLoader();
            } else {
                loader = resource.getClassLoader();
                Thread.currentThread().setContextClassLoader(resource.getClassLoader());
                pluginClass = loader.loadClass(className);
            }
            instantiate();
        } catch (ClassNotFoundException e) {
            throw new org.geworkbench.util.BaseRuntimeException("Could not instantiate plugin: " + className, e);
        } finally {
            Thread.currentThread().setContextClassLoader(defaultClassLoader);
        }
    }

    // ---------------------------------------------------------------------------
    // --------------- Methods
    // ---------------------------------------------------------------------------

    private void instantiate() {
        ComponentRegistry componentRegistry = ComponentRegistry.getRegistry();
        ClassLoader defaultClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (resource != null) {
                Thread.currentThread().setContextClassLoader(resource.getClassLoader());
                log.debug("Set context class loader.");
            }
            plugin = componentRegistry.createComponent(pluginClass, this);
            log.debug("Created component: " + getLabel() + ", " + pluginClass);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate plugin:" + pluginClass, e);
        } finally {
            Thread.currentThread().setContextClassLoader(defaultClassLoader);
        }
    }

    /**
     * Definition of equality for <code>PluginDescriptor</code> objects. Checks
     * for equality will be performed when attempting to add component descriptors
     * in the {@link org.geworkbench.engine.config.PluginRegistry PluginRegistry}.
     *
     * @param obj
     * @return
     */
    public boolean equals(Object obj) {
    	if(!(obj instanceof PluginDescriptor)) return false;
        return this.id.equals( ((PluginDescriptor) obj).id );
    }
    
    public int hashCode() {
    	return id.hashCode();
    }

    public int compareTo(PluginDescriptor other) {
    	return id.compareToIgnoreCase(other.id);
    }

    public Object getModule(String moduleMethod) {
        return modules.get(moduleMethod);
    }

    public void setModule(String moduleMethod, Object module) {
        modules.put(moduleMethod, module);
    }

    public String getModuleID(String moduleMethod) {
        return moduleMappings.get(moduleMethod);
    }

    public void setModuleID(String moduleMethod, String id) {
        moduleMappings.put(moduleMethod, id);
    }

    void setPlugin(Object pgIn) {
        this.plugin = pgIn;
    }

    public Object getPlugin() {
        return plugin;
    }

    public Class<?> getPluginClass() {
        return pluginClass;
    }

    public ClassLoader getClassLoader(){
        return loader;
    }

    public int getPreferredOrder() {
        return preferredOrder;
    }

    /**
     * Registers an <code>ActionListener</code> with a <code>JMenuItem</code>.
     *
     * @param mItem A menu item.
     * @param al    An <code>ActionListener</code>.
     * @param mode  Designates when the <code>ActionListerner</code> will be
     *              notified.
     */
    public void addMenuListener(JMenuItem mItem, ActionListener al, String mode) throws MalformedMenuItemException {
        // Register the ActionListener with the menu item.
        HashMap<JMenuItem, Vector<ActionListener>> hMap = null;
        if(mode.equals("onFocus")) {
        	hMap = onFocusMenuItems;
        } else {
        	hMap = alwaysMenuItems;
        }
        if (!hMap.containsKey(mItem))
            hMap.put(mItem, new Vector<ActionListener>());
        Vector<ActionListener> actionListeners = hMap.get(mItem);
        if (!actionListeners.contains(al))
            actionListeners.add(al);
    }

    /**
     * Checks if the plugin implements the <code>MenuListener</code> interface.
     *
     * @return The result of the test
     */
    public boolean isMenuListener() {
        try {
            if (!Class.forName("org.geworkbench.engine.config.MenuListener").isAssignableFrom(plugin.getClass()))
                return false;
            else
                return true;
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace(System.err);
            return false;
        }

    }

    /**
     * Checks if the plugin implements the <code>VisualPlugin</code> interface.
     *
     * @return The result of the test.
     */
    public boolean isVisualPlugin() {
        try {
            if (!Class.forName("org.geworkbench.engine.config.VisualPlugin").isAssignableFrom(pluginClass))
                return false;
            else
                return true;
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace(System.err);
            return false;
        }

    }

    /**
     * Invoked when the plugin (which must implement the <code>VisualPlugin</code>
     * interface) looses the keyboard focus. When this happens, this method takes care
     * of removing the "onFocus" ActionListeners that this plugin has associated with
     * various menu items.
     */
    public void disableFocusMenuItems() {
        for (JMenuItem menuItem: onFocusMenuItems.keySet()) {
        	Vector<ActionListener> actionListeners = onFocusMenuItems.get(menuItem);
            int len = actionListeners.size();
            for (int i = 0; i < len; ++i)
                menuItem.removeActionListener((ActionListener) actionListeners.get(i));
            // If there are no more ActionListeners associated with the menu item,
            // make the menu item unselectable.
            if (menuItem.getActionListeners().length == 0)
                menuItem.setEnabled(false);
        }

    }

    /**
     * Invoked when the plugin (which must implement the <code>VisualPlugin</code>
     * interface) gains the keyboard focus. When this happens, this method takes care
     * of removing the "onFocus" ActionListeners that this plugin has associated with
     * various menu items.
     */
    public void enableFocusMenuItems() {
        for (JMenuItem menuItem : onFocusMenuItems.keySet()) {
        	Vector<ActionListener> actionListeners = onFocusMenuItems.get(menuItem);
            int len = actionListeners.size();
            for (int i = 0; i < len; ++i)
                menuItem.addActionListener((ActionListener) actionListeners.get(i));
            if (menuItem.getActionListeners().length > 0)
                menuItem.setEnabled(true);
        }

    }

    /**
     * add menuitem info into an arraylist
     *
     * @param path        String
     * @param mode        String
     * @param var         String
     * @param icon        String
     * @param accelerator String
     */
    public void addMenuItemInfo(String path, String mode, String var, String icon, String accelerator) {
        HashMap<String, String> menu = new HashMap<String, String>();
        menu.put("path", path);
        menu.put("mode", mode);
        menu.put("var", var);
        menu.put("icon", icon);
        menu.put("accelerator", accelerator);
        menuItemInfos.add(menu);
    }

    /**
     * returns the menuitem infos
     *
     * @return ArrayList
     */
    public ArrayList<HashMap<String, String>> getMenuItemInfos() {
        return menuItemInfos;
    }

    public String getVisualLocation() {
        return visualLocation;
    }

    public void setVisualLocation(String visualLocation) {
        this.visualLocation = visualLocation;
    }

    public void addTypeToSubscriptionIgnoreSet(Class<?> type) {
        subscriptionIgnoreSet.add(type);
    }

    public boolean isInSubscriptionIgnoreSet(Class<?> type) {
        return subscriptionIgnoreSet.contains(type);
    }

    public void setComponentMetadata(ComponentMetadata metadata) throws NotMenuListenerException, MalformedMenuItemException, NotVisualPluginException {

        //// Set up menu items
        List<MenuItemInfo> menuItems = metadata.getMenuInfoList();
        for (int i = 0; i < menuItems.size(); i++) {
            MenuItemInfo menuItemInfo = menuItems.get(i);
            PluginObject.registerMenuItem(this,
                    menuItemInfo.getPath(),
                    menuItemInfo.getMode(),
                    menuItemInfo.getVar(),
                    menuItemInfo.getIcon(),
                    menuItemInfo.getAccelerator());
        }
    }

    public String toString() {
        return getID() + ": " + getLabel();
    }
    
    // TODO it is IdentifiableImpl's responsibility to check this
	public static Vector<String> getUsedIds(){
    	return usedIds;
    }
 
    // TODO bad idea to expose this member directly for a specific need from ccm
    public ComponentResource getResource() {
		return resource;
	}
    
}