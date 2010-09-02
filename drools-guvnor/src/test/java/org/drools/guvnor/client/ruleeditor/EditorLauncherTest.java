/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.guvnor.client.ruleeditor;
/*
 * Copyright 2005 JBoss Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import junit.framework.TestCase;

import org.drools.guvnor.client.common.AssetFormats;

public class EditorLauncherTest extends TestCase {

	public void testDummy() {
		
	}
	
    public void FIXMEtestIcons()  {
        assertNotNull(EditorLauncher.TYPE_IMAGES);
        assertNotNull(EditorLauncher.getAssetFormatIcon( "drl" ));
        assertNotNull(EditorLauncher.getAssetFormatIcon( "JKLGFJSLKGJFDLKGJFKLDJGLFKDJGKLFD" ));
        assertEquals("model_asset.gif", EditorLauncher.getAssetFormatIcon( AssetFormats.MODEL ));
    }
    
}