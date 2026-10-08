package net.fabricmc.loader.impl.lib.sat4j.minisat.core;

import net.fabricmc.loader.impl.lib.sat4j.specs.ISolver;
import net.fabricmc.loader.impl.lib.sat4j.specs.UnitPropagationListener;

public interface ICDCL extends ActivityListener, Learner, ISolver, UnitPropagationListener {
}
