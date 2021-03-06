package me.nonit.nicky;

import me.nonit.nicky.commands.DelNickCommand;
import me.nonit.nicky.commands.NickCommand;
import me.nonit.nicky.commands.NickyCommand;
import me.nonit.nicky.commands.RealNameCommand;
import me.nonit.nicky.databases.MySQL;
import me.nonit.nicky.databases.SQL;
import me.nonit.nicky.databases.SQLite;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.FileConfigurationOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;

public class Nicky extends JavaPlugin
{
    private static String PREFIX;

    private final Set<SQL> databases;
    private static SQL DATABASE;

    private static boolean TAGAPI = false;
    private static boolean TABS;
    private static boolean UNIQUE;
    private static String NICK_PREFIX;
    private static int LENGTH;
    private static int MIN_LENGTH;
    private static String CHARACTERS;
    private static List<String> BLACKLIST;

    public Nicky()
    {
        databases = new HashSet<SQL>();
    }

    @Override
    public void onEnable()
    {
        databases.add( new MySQL( this ) );
        databases.add( new SQLite( this ) );

        setupConfig();

        setupDatabase();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents( new PlayerListener(), this );

        if( pm.isPluginEnabled( "TagAPI" ) && getConfig().getBoolean( "tagapi" ) )
        {
            pm.registerEvents( new TagAPIListener( this ), this );
            log( "TagAPI link enabled." );
            TAGAPI = true;
        }

        BLACKLIST = new ArrayList<String>();
        reloadNickyConfig();

        getCommand( "nick" ).setExecutor( new NickCommand( this ) );
        getCommand( "delnick" ).setExecutor( new DelNickCommand( this ) );
        getCommand( "realname" ).setExecutor( new RealNameCommand() );
        getCommand( "nicky" ).setExecutor( new NickyCommand( this ) );

        if( ! DATABASE.checkConnection() )
        {
            log( "Error with DATABASE" );
            pm.disablePlugin( this );
        }

        loadMetrics();
    }

    @Override
    public void onDisable()
    {
        DATABASE.disconnect();
    }

    public void reloadNickyConfig()
    {
        super.reloadConfig();

        FileConfiguration config = getConfig();

        try
        {
            PREFIX = ChatColor.YELLOW + ChatColor.translateAlternateColorCodes( '&', config.get( "nicky_prefix" ).toString() ) + ChatColor.GREEN + " ";

            // Database info not set in this class.

            TABS = config.getBoolean( "tab" );
            UNIQUE = config.getBoolean( "unique" );
            NICK_PREFIX = config.get( "prefix" ).toString();
            LENGTH = Integer.parseInt( config.get( "length" ).toString() );
            MIN_LENGTH = Integer.parseInt( config.get( "min_length" ).toString() );
            CHARACTERS = config.get( "characters" ).toString();

            BLACKLIST.clear();
            BLACKLIST = config.getStringList( "blacklist" );
        }
        catch( Exception e )
        {
            log( "Warning - You have an error in your config." );
        }

        for( Player player : Bukkit.getServer().getOnlinePlayers() )
        {
            Nick nick = new Nick( player );

            nick.load();
        }
    }

    private void setupConfig()
    {
        saveDefaultConfig();

        FileConfiguration config = getConfig();

        // Update header.
        config.options().copyHeader();

        if( ! config.isSet( "nicky_prefix" ) )
        {
            config.set( "nicky_prefix", "[Nicky]" );
        }

        // Database config
        if( ! config.isSet( "type" ) )
        {
            config.set( "type", "sqlite" );
        }
        if( ! config.isSet( "host" ) )
        {
            config.set( "host", "localhost" );
        }
        if( ! config.isSet( "port" ) )
        {
            config.set( "port", "3306" );
        }
        if( ! config.isSet( "user" ) )
        {
            config.set( "user", "root" );
        }
        if( ! config.isSet( "password" ) )
        {
            config.set( "password", "password" );
        }
        if( ! config.isSet( "database" ) )
        {
            config.set( "database", "nicky" );
        }

        // Settings
        if( ! config.isSet( "tagapi" ) )
        {
            config.set( "tagapi", true );
        }
        if( ! config.isSet( "tab" ) )
        {
            config.set( "tab", true );
        }
        if( ! config.isSet( "unique" ) )
        {
            config.set( "unique", true );
        }
        if( ! config.isSet( "prefix" ) )
        {
            config.set( "prefix", "&e~" );
        }
        if( ! config.isSet( "length" ) )
        {
            config.set( "length", 20 );
        }
        if( ! config.isSet( "min_length" ) )
        {
            config.set( "min_length", 3 );
        }
        if( ! config.isSet( "characters" ) )
        {
            config.set( "characters", "[^a-zA-Z0-9§]" );
        }
        if( ! config.isSet( "blacklist" ) )
        {
            List<String> listOfStrings = Arrays.asList( "Melonking", "Admin" );
            config.set( "blacklist", listOfStrings );
        }

        saveConfig();
    }

    private void loadMetrics()
    {
        try
        {
            Metrics metrics = new Metrics(this);

            Metrics.Graph graphDatabaseType = metrics.createGraph( "Database Type" );

            graphDatabaseType.addPlotter( new Metrics.Plotter( DATABASE.getConfigName() ) {
                @Override
                public int getValue() {
                    return 1;
                }
            } );

            Metrics.Graph graphTagAPI = metrics.createGraph( "TagAPI" );

            String graphTagAPIValue = "No";
            if( TAGAPI )
            {
                graphTagAPIValue = "Yes";
            }

            graphTagAPI.addPlotter( new Metrics.Plotter( graphTagAPIValue ) {
                @Override
                public int getValue() {
                    return 1;
                }
            } );

            metrics.start();
        }
        catch (IOException e)
        {
            // Failed to submit the stats :-(
        }
    }

    private boolean setupDatabase()
    {
        String type = getConfig().getString("type");

        DATABASE = null;

        for ( SQL database : databases )
        {
            if ( type.equalsIgnoreCase( database.getConfigName() ) )
            {
                DATABASE = database;

                log( "Database set to " + database.getConfigName() + "." );

                break;
            }
        }

        if ( DATABASE == null)
        {
            log( "Database type does not exist!" );

            return false;
        }

        return true;
    }

    public void log( String message )
    {
        getLogger().info( message );
    }

    public static SQL getNickDatabase() { return DATABASE; }

    public static String getPrefix() { return PREFIX; }

    public static boolean isTagAPIUsed() { return TAGAPI; }

    public static boolean isTabsUsed() { return TABS; }

    public static boolean isUnique() { return UNIQUE; }

    public static String getNickPrefix() { return NICK_PREFIX; }

    public static List<String> getBlacklist() { return BLACKLIST; }

    public static int getLength() { return LENGTH; }

    public static int getMinLength() { return MIN_LENGTH; }

    public static String getCharacters() { return CHARACTERS; }

    public static String translateNormalColorCodes( String textToTranslate )
    {
        char[] b = textToTranslate.toCharArray();
        for( int i = 0; i < b.length - 1; i++ )
        {
            if( b[i] == '&' && "0123456789AaBbCcDdEeFfRr".indexOf( b[i + 1] ) > -1 )
            {
                b[i] = ChatColor.COLOR_CHAR;
                b[i + 1] = Character.toLowerCase( b[i + 1] );
            }
        }
        return new String( b );
    }

    public static String translateExtraColorCodes( String textToTranslate )
    {
        char[] b = textToTranslate.toCharArray();
        for( int i = 0; i < b.length - 1; i++ )
        {
            if( b[i] == '&' && "KkLlMmNnOoRr".indexOf( b[i + 1] ) > -1 )
            {
                b[i] = ChatColor.COLOR_CHAR;
                b[i + 1] = Character.toLowerCase( b[i + 1] );
            }
        }
        return new String( b );
    }
}