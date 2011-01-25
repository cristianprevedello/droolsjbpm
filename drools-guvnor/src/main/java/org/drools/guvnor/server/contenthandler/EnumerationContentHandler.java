/*
 * Copyright 2007 JBoss Inc
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

package org.drools.guvnor.server.contenthandler;

import java.util.ArrayList;
import java.util.List;

import org.drools.guvnor.client.rpc.BuilderResult;
import org.drools.guvnor.client.rpc.BuilderResultLine;
import org.drools.guvnor.client.rpc.RuleAsset;
import org.drools.guvnor.client.rpc.RuleContentText;
import org.drools.ide.common.server.util.DataEnumLoader;
import org.drools.repository.AssetItem;

public class EnumerationContentHandler extends PlainTextContentHandler
    implements
    IValidating {

    public BuilderResult validateAsset(AssetItem asset) {

        String content = asset.getContent();
        DataEnumLoader loader = new DataEnumLoader( content );
        if ( !loader.hasErrors() ) {
            return new BuilderResult();
        } else {
            List<BuilderResultLine> errors = new ArrayList<BuilderResultLine>();
            List<String> errs = loader.getErrors();

            for ( String message : errs ) {

                BuilderResultLine result = new BuilderResultLine();
                result.assetName = asset.getName();
                result.assetFormat = asset.getFormat();
                result.uuid = asset.getUUID();
                result.message = message;
                errors.add( result );
            }

            BuilderResult result = new BuilderResult();
            result.setLines( errors.toArray( new BuilderResultLine[errors.size()] ) );

            return result;
        }
    }

    public BuilderResult validateAsset(RuleAsset asset) {
        String content = ((RuleContentText) asset.content).content;
        DataEnumLoader loader = new DataEnumLoader( content );
        if ( !loader.hasErrors() ) {
            return new BuilderResult();
        } else {
            List<BuilderResultLine> errors = new ArrayList<BuilderResultLine>();
            List<String> errs = loader.getErrors();

            for ( String message : errs ) {

                BuilderResultLine result = new BuilderResultLine();
                result.assetName = asset.metaData.name;
                result.assetFormat = asset.metaData.format;
                result.uuid = asset.uuid;
                result.message = message;
                errors.add( result );
            }

            BuilderResult result = new BuilderResult();
            result.setLines( errors.toArray( new BuilderResultLine[errors.size()] ) );

            return result;
        }
    }

}
