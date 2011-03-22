/*
 *  Copyright 2007-2008, Plutext Pty Ltd.
 *   
 *  This file is part of docx4j.

    docx4j is licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 

    You may obtain a copy of the License at 

        http://www.apache.org/licenses/LICENSE-2.0 

    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS, 
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.

    This file is a translation into Java of a portion of 
    Program.cs from Microsoft's OpenXmlViewer, which was made available to
    the project under the following license:
    
		(c) Microsoft Corporation
		
		Microsoft Public License (Ms-PL)
		
		This license governs use of the accompanying software. If you use the
		software, you accept this license. If you do not accept the license, do
		not use the software.
		
		1. Definitions
		
		The terms "reproduce," "reproduction," "derivative works," and "distribution"
		have the same meaning here as under U.S. copyright law.
		
		A "contribution" is the original software, or any additions or changes to the software.
		
		A "contributor" is any person that distributes its contribution under this license.
		
		"Licensed patents" are a contributor's patent claims that read directly on its contribution.
		
		2. Grant of Rights
		
		(A) Copyright Grant- Subject to the terms of this license, including the
		license conditions and limitations in section 3, each contributor
		grants you a non-exclusive, worldwide, royalty-free copyright license
		to reproduce its contribution, prepare derivative works of its
		contribution, and distribute its contribution or any derivative works
		that you create.
		
		(B) Patent Grant- Subject to the terms of this
		license, including the license conditions and limitations in section 3,
		each contributor grants you a non-exclusive, worldwide, royalty-free
		license under its licensed patents to make, have made, use, sell, offer
		for sale, import, and/or otherwise dispose of its contribution in the
		software or derivative works of the contribution in the software.
		
		3. Conditions and Limitations
		
		(A) No Trademark License- This license does not grant you rights to use any contributors' name, logo, or trademarks.
		
		(B) If you bring a patent claim against any contributor over patents that
		you claim are infringed by the software, your patent license from such
		contributor to the software ends automatically.
		
		(C) If you distribute any portion of the software, you must retain all copyright,
		patent, trademark, and attribution notices that are present in the
		software.
		
		(D) If you distribute any portion of the software in
		source code form, you may do so only under this license by including a
		complete copy of this license with your distribution. If you distribute
		any portion of the software in compiled or object code form, you may
		only do so under a license that complies with this license.
		
		(E) The software is licensed "as-is." You bear the risk of using it. The
		contributors give no express warranties, guarantees or conditions. You
		may have additional consumer rights under your local laws which this
		license cannot change. To the extent permitted under your local laws,
		the contributors exclude the implied warranties of merchantability,
		fitness for a particular purpose and non-infringement.

 */
package org.docx4j.model.listnumbering;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.docx4j.wml.Lvl;
import org.docx4j.wml.Numbering;

public class ListNumberingDefinition {
	
	/* There should be only one Emulator object per 
	 * WordprocessingML package.  It is set on the 
	 * numbering part.
	 */
	
	// The underlying JAXB object 
	private Numbering.Num numNode;
	public Numbering.Num getNumNode() {
		return numNode;
	}
	
	protected static Logger log = Logger.getLogger(ListNumberingDefinition.class);
	
    /// <summary>
    /// constructor
    /// </summary>
    /// <param name="numNode"></param>
    /// <param name="nsm"></param>
    /// <param name="abstractListDefinitions"></param>
    public ListNumberingDefinition(Numbering.Num numNode, 
    		HashMap<String, AbstractListNumberingDefinition> abstractListDefinitions)
    {
    	this.numNode = numNode;
    	
        this.listNumberId =  numNode.getNumId().toString(); //getAttributeValue(numNode, "w:numId");

        //XmlNode abstractNumNode = numNode.SelectSingleNode("./w:abstractNumId", nsm);
        Numbering.Num.AbstractNumId abstractNumNode = numNode.getAbstractNumId();
        if (abstractNumNode == null) {
        	log.warn("No abstractNumId on w:numId=" + listNumberId);
        } else {
            this.abstractListDefinition = abstractListDefinitions.get(abstractNumNode.getVal().toString() ); //[getAttributeValue(abstractNumNode, ValAttrName)];
            if (abstractListDefinition==null) {
            	log.warn("No abstractListDefinition for w:numId=" + listNumberId);            	
            }

            this.levels = new HashMap<String, ListLevel>(this.abstractListDefinition.getLevelCount() );

            // initialize the levels to the same as the template ("abstract") list level
    		Iterator listLevelIterator = this.abstractListDefinition.getListLevels().entrySet().iterator();
    	    while (listLevelIterator.hasNext()) {
    	        Map.Entry pairs = (Map.Entry)listLevelIterator.next();
    	        this.levels.put( (String)pairs.getKey(), new ListLevel( (ListLevel)pairs.getValue() ) );        	        
    	    }

            // propagate the level overrides into the current list number level definition
            // XmlNodeList levelOverrideNodes = numNode.SelectNodes("./w:lvlOverride", nsm);

            List<Numbering.Num.LvlOverride> levelOverrideNodes = numNode.getLvlOverride(); 
            if (levelOverrideNodes != null)
            {
                for (Numbering.Num.LvlOverride overrideNode : levelOverrideNodes)
                {
                    //XmlNode node = overrideNode.SelectSingleNode("./w:lvl", nsm);
                	Lvl node = overrideNode.getLvl();
                    if (node != null)
                    {
                        String overrideLevelId = node.getIlvl().toString(); //getAttributeValue(node, "w:ilvl");

                        if (overrideLevelId!=null && !overrideLevelId.equals("") )
                        {
                            this.levels.get(overrideLevelId).SetOverrides(node);
                        }
                    }
                }
            }
        }
    }

    private AbstractListNumberingDefinition abstractListDefinition;
    public AbstractListNumberingDefinition getAbstractListDefinition() {
		return abstractListDefinition;
	}

	private HashMap<String, ListLevel> levels;
	public ListLevel getLevel(String ilvl) {
		return levels.get(ilvl);
	}
    

    /// <summary>
    /// increment the occurrence count of the specified level, reset the occurrence count of derived levels
    /// </summary>
    /// <param name="level"></param>
    public void IncrementCounter(String level)
    {
    	log.debug("Increment level " + level);
        this.levels.get(level).IncrementCounter();

        // Now set all lower levels back to 1.
        
        // here's a bit where the decision to use Strings as level IDs was bad 
        // - I need to loop through the derived levels and reset their counters
        //UInt32 levelNumber = System.Convert.ToUInt32(level, CultureInfo.InvariantCulture) + 1;
        int levelNumber = Integer.parseInt(level)+1;
        String levelString =  Integer.toString(levelNumber);

        while (this.levels.containsKey(levelString))
        {
        	log.debug("Reset level " + levelNumber);
            this.levels.get(levelString).ResetCounter();
            levelNumber++;
            levelString = Integer.toString(levelNumber);
        }
    }

    private String listNumberId;

    /// <summary>
    /// numId of this list numbering schema
    /// </summary>
    public String getListNumberId() 
        {
            return this.listNumberId;
        }

    /// <summary>
    /// returns a String containing the current state of the counters, up to the indicated level
    /// </summary>
    /// <param name="level"></param>
    /// <returns></returns>
    public String GetCurrentNumberString(String level)
    {
        String formatString = this.levels.get(level).getLevelText();
        log.debug("levelText: " + formatString );
        StringBuilder result = new StringBuilder();
        String temp = ""; //String.Empty;

        for (int i = 0; i < formatString.length(); i++)
        {
            //temp = formatString.SubString(i, 1);
        	// C# pos i, length 1            	
        	temp = formatString.substring(i, i+1);
            if (temp.equals("%") )
            {
                if (i < formatString.length() - 1)
                {
                    String formatStringLevel = formatString.substring(i + 1, i+2);
                    // as it turns out, in the format String, the level is 1-based
                    int levelId =  Integer.parseInt(formatStringLevel) - 1;
                    result.append(this.levels.get( Integer.toString(levelId) ).getCurrentValueFormatted() );
                    i++;
                }
            }
            else
            {
                result.append(temp);
            }
        }

        return result.toString();
    }

    /// <summary>
    /// retrieve the font name that was specified for the list String
    /// </summary>
    /// <param name="level"></param>
    /// <returns></returns>
    public String GetFont(String level)
    {
        return this.levels.get(level).getFont();
    }

    /// <summary>
    /// retrieve whether the level was a bullet list type
    /// </summary>
    /// <param name="level"></param>
    /// <returns></returns>
    public boolean IsBullet(String level)
    {
        return this.levels.get(level).IsBullet();
    }

    /// <summary>
    /// returns whether the specific level ID exists - in testing we've seen some referential integrity issues due to Word bugs
    /// </summary>
    /// <param name="level">
    /// </param>
    /// <returns>
    /// </returns>
    /// <id guid="b94c13b8-7273-4f6a-927b-178d685fbe0f" />
    /// <owner alias="ROrleth" />
    public boolean LevelExists(String level)
    {
        return this.levels.containsKey(level);
    }

}

//    static String getAttributeValue(XmlNode node, String name)
//    {
//        String value = String.Empty;
//
//        XmlAttribute attribute = node.Attributes[name];
//        if (attribute != null && attribute.Value != null)
//        {
//            value = attribute.Value;
//        }
//
//        return value;
//    }
