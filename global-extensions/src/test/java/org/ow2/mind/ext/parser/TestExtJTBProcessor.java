/**
 * Copyright (C) 2013 Schneider-Electric
 *
 * This file is part of "Mind Compiler" is free software: you can redistribute 
 * it and/or modify it under the terms of the GNU Lesser General Public License 
 * as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact: mind@ow2.org
 *
 * Authors: Stephane Seyvoz
 * Contributors: 
 */

package org.ow2.mind.ext.parser;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import org.objectweb.fractal.adl.Definition;
import org.ow2.mind.CommonFrontendModule;
import org.ow2.mind.adl.AbstractADLFrontendModule;
import org.ow2.mind.ext.parser.ExtJTBProcessor;
import org.ow2.mind.plugin.PluginLoaderModule;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class TestExtJTBProcessor {

  protected static final String DTD = "classpath://org/ow2/mind/adl/mind_v1.dtd";
  ExtJTBProcessor                  processor;

  @BeforeMethod(alwaysRun = true)
  protected void setUp() throws Exception {

    final Injector injector = Guice.createInjector(new CommonFrontendModule(),
        new PluginLoaderModule(), new AbstractADLFrontendModule() {
        });

    processor = injector.getInstance(ExtJTBProcessor.class);
  }

  protected InputStream getEXT(final String fileName) throws Exception {
    final ClassLoader loader = getClass().getClassLoader();
    final InputStream is = loader.getResourceAsStream(fileName);
    assertNotNull(is, "Can't find input file \"" + fileName + "\"");
    return is;
  }

  @Test(groups = {"functional"})
  public void test1() throws Exception {
    final List<Definition> node = processor.parseEXT(getEXT("all-static.ext"), "Test1",
        "Test1.ext");
    assertTrue(node instanceof List<?>);
  }
}
