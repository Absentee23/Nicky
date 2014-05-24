package me.nonit.nicky;

import me.nonit.nicky.databases.SQL;
import org.bukkit.entity.Player;

public class Nick
{
    private Player player;
    private SQL database;
    private String uuid;

    public Nick(Nicky plugin, Player player)
    {
        this.player = player;
        database = plugin.getNickDatabase();

        this.uuid = player.getUniqueId().toString();
    }

    public boolean loadNick()
    {
        String nickname = getNick();

        if( nickname != null )
        {
            player.setDisplayName( nickname );
            return true;
        }

        return false;
    }

    public void unLoadNick()
    {
        unSetNick();
        player.setDisplayName( player.getName() );
    }

    public void setNick( String nick )
    {
        if( getNick() != null )
        {
            unSetNick();
        }

        database.uploadNick( uuid, nick );
    }

    private String getNick()
    {
        return database.downloadNick( uuid );
    }

    private void unSetNick()
    {
        database.deleteNick( uuid );
    }
}