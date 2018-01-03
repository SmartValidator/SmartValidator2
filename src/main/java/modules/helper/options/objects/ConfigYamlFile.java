package modules.helper.options.objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ConfigYamlFile
{
    @JsonProperty
    private DatabaseProps database;

    @JsonProperty
    private General general;

    @JsonProperty
    private Logger logger;

    public DatabaseProps getDatabase() {
        return database;
    }

    public void setDatabase(DatabaseProps database) {
        this.database = database;
    }

    public General getGeneral() {
        return general;
    }

    public void setGeneral(General general) {
        this.general = general;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }
}
