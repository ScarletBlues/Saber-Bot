package io.schedulerbot.commands.general;

import io.schedulerbot.Main;
import io.schedulerbot.commands.Command;
import io.schedulerbot.utils.MessageUtilities;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Collection;

/**
 * the command which causes the bot to message the event's parent user with
 * the bot operation command list/guide. Attempts to remove the '!help' message
 * if the message does not originate from a private channel
 */
public class HelpCommand implements Command
{
    private static String prefix = Main.getBotSettings().getCommandPrefix();

    private final String INTRO = "I am **" + Main.getBotSelfUser().getName() + "**, the task scheduling discord bot." +
            " I can provide your discord with basic event schedule management.  Invite me to your discord and set up " +
            "my appropriate channels to get started.\n\n";

    private static final String USAGE_EXTENDED = "\nTo get detailed information concerning the usage of any of these" +
            " commands use the command **!help <command>** where the prefix for <command> is stripped off. " +
            "Ex. **!help create**";

    private static final String USAGE_BRIEF = "**" + prefix + "help** - Messages the user help messages.";

    @Override
    public String help(boolean brief)
    {
        if( brief )
            return USAGE_BRIEF;
        else
            return USAGE_BRIEF + "\n" + USAGE_EXTENDED;
    }

    @Override
    public String verify(String[] args, MessageReceivedEvent event)
    {
        if(args.length>1)
            return "Not enough arguments";
        return "";
    }

    @Override
    public void action(String[] args, MessageReceivedEvent event)
    {
        Collection<Command> commands = Main.commandHandler.getCommands();

        // send the bot intro with a brief list of commands to the user
        if(args.length < 1)
        {
            String commandsBrief = ""; for( Command cmd : commands )
                commandsBrief += cmd.help( true ) + "\n";

            MessageUtilities.sendPrivateMsg( INTRO + "__**Available commands**__\n" +
                    commandsBrief + USAGE_EXTENDED, event.getAuthor(), null );
        }
        // otherwise read search the commands for the first arg
        else
        {
            Command cmd = Main.commandHandler.getCommand( args[0] );
            if( cmd != null )
            {
                String helpMsg = cmd.help(false);
                MessageUtilities.sendPrivateMsg(helpMsg, event.getAuthor(), null);
            }
        }

        if( !event.isFromType(ChannelType.PRIVATE) )
            MessageUtilities.deleteMsg( event.getMessage(), null );
    }
}
