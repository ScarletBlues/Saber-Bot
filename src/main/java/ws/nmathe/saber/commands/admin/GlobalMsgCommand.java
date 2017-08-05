package ws.nmathe.saber.commands.admin;

import net.dv8tion.jda.core.entities.MessageChannel;
import ws.nmathe.saber.Main;
import ws.nmathe.saber.commands.Command;
import ws.nmathe.saber.utils.MessageUtilities;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Collection;

/**
 */
public class GlobalMsgCommand implements Command
{

    @Override
    public String name()
    {
        return "announcement";
    }

    @Override
    public String help(String prefix, boolean brief)
    {
        return null;
    }

    @Override
    public String verify(String prefix, String[] args, MessageReceivedEvent event)
    {
        return "";
    }

    @Override
    public void action(String prefix, String[] args, MessageReceivedEvent event)
    {
        String msg = "";
        for( String arg : args )
        {
            msg += arg + " ";
        }

        for( Guild guild : Main.getBotJda().getGuilds() )
        {
            String channelId = Main.getGuildSettingsManager().getGuildSettings(guild.getId()).getCommandChannelId();
            if(channelId == null) // look for default control channel name
            {
                Collection<TextChannel> chans = guild.getTextChannelsByName( Main.getBotSettingsManager().getControlChan(), true );
                for( TextChannel chan : chans )
                {
                    MessageUtilities.sendMsg(msg, chan, null);
                }
            }
            else // send to configured control channel
            {
                MessageChannel chan = guild.getTextChannelById(channelId);
                if(chan != null)
                {
                    MessageUtilities.sendMsg(msg, chan, null);
                }
            }
        }
        MessageUtilities.sendPrivateMsg("Finished sending announcements to guilds!", event.getAuthor(), null);
    }
}
