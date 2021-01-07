package net.avdw.property;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.tinylog.Logger;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * A single class property file loader.
 * <p>
 * The aim is to have one class that can be copied between projects.
 * The reason is that I hate dependency management on my own classes.
 * I have no problem with duplication, it makes code more modular.
 *
 * @version 2021-01-07 Changed log messages to read cleaner
 * 2020-12-11 Added default constructor which does not set namespace
 * 2020-08-24 One class property loader
 */
public class PropertyFile {
    private final String namespace;

    public PropertyFile() {
        namespace = "namespace-not-set";
    }

    @Inject
    public PropertyFile(@Named("namespace") final String namespace) {
        this.namespace = namespace;
    }

    /**
     * Read all property files adhering to the following load order:
     * - jar    : search for the property file on the classpath
     * - local  : search for property file in the application directory
     * - global : search for property file from a system directory
     * Thus global properties overwrite local properties which overwrite jar properties.
     *
     * @param name the name of the property file
     * @return Properties adhering to the load order
     */
    public Properties read(final String name) {
        String propertyFilename = String.format("%s.properties", name);
        Properties properties = new Properties();

        String classPath = String.format("/%s", propertyFilename);
        try (InputStream is = PropertyFile.class.getResourceAsStream(classPath)) {
            Properties prop = new Properties();
            prop.load(is);
            properties.putAll(prop);
            int width = prop.keySet().stream().mapToInt(k -> k.toString().length()).max().orElseThrow();
            Logger.debug("Classpath properties ({}):" +
                    "\n{}", classPath, prop.entrySet().stream()
                    .map(e -> String.format("%-" + width + "s = %s", e.getKey(), e.getValue()))
                    .sorted()
                    .collect(Collectors.joining("\n")));
        } catch (IOException e) {
            Logger.trace("No classpath properties (classpath://{})", classPath);
            throw new UnsupportedOperationException();
        }

        Path localPath = Paths.get(propertyFilename);
        try (FileReader reader = new FileReader(localPath.toString(), StandardCharsets.UTF_8)) {
            Properties prop = new Properties();
            prop.load(reader);
            properties.putAll(prop);
            Logger.debug("Local properties ({}):" +
                    "\n{}", localPath, prop);
        } catch (IOException e) {
            Logger.trace("No local properties ({})", localPath);
        }

        Path globalPath = Paths.get(System.getProperty("user.home")).resolve(namespace).resolve(propertyFilename);
        try (FileReader reader = new FileReader(globalPath.toString(), StandardCharsets.UTF_8)) {
            Properties prop = new Properties();
            prop.load(reader);
            properties.putAll(prop);
            Logger.debug("Global properties ({}):" +
                    "\n{}", globalPath, prop);
        } catch (IOException e) {
            Logger.trace("No global properties ({})", globalPath);
        }
        return properties;
    }
}
