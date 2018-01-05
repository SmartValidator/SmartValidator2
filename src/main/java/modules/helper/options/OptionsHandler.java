package modules.helper.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import modules.helper.options.objects.ConfigYamlFile;

import java.io.*;
import java.net.URL;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OptionsHandler {
    private static OptionsHandler instance = null;
    final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    public ConfigYamlFile getOptions() {
        return options;
    }

    private ConfigYamlFile options;

    private OptionsHandler(){

        URL url = getClass().getResource("/config.yaml");

        String configFilePath = url.getPath();

        try  {
            File file = new File(configFilePath);
            rwl.writeLock().lock();
            final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()); // jackson databind
            options = mapper.readValue(file, ConfigYamlFile.class);
            System.out.println(options.getDatabase());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            rwl.writeLock().unlock();
        }
    }



    public static synchronized OptionsHandler getInstance(){
        if(instance == null){
            instance = new OptionsHandler();
        }
        return instance;
    }
}
