package com.mrmag518.spawnergui.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.mrmag518.spawnergui.SpawnerGUI;

public class ReloadCommand implements CommandExecutor {

	private final SpawnerGUI i;

	public ReloadCommand(SpawnerGUI i)
	{
		this.i = i;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		this.i.reloadConfig();
		this.i.eatGUIs();

		return true;
	}

}