/*
 * Copyright 2012 Roman Nurik
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nover.yana.assistant_installation;

import fr.nover.yana.assistant_installation.wizard.model.AbstractWizardModel;
import fr.nover.yana.assistant_installation.wizard.model.BranchPage;
import fr.nover.yana.assistant_installation.wizard.model.CustomerInfoPage;
import fr.nover.yana.assistant_installation.wizard.model.IPAdressInfoPage;
import fr.nover.yana.assistant_installation.wizard.model.IPAdress_ExtInfoPage;
import fr.nover.yana.assistant_installation.wizard.model.MultipleFixedChoicePage;
import fr.nover.yana.assistant_installation.wizard.model.PageList;
import fr.nover.yana.assistant_installation.wizard.model.SingleFixedChoicePage;

import android.content.Context;

public class Stockage_AI extends AbstractWizardModel {
    public Stockage_AI(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewRootPageList() {
        return new PageList(
        		new CustomerInfoPage(this, "Qui êtes-vous ?")
                	.setRequired(true),
                new SingleFixedChoicePage(this, "Vous êtes un(e)...")
                    .setChoices("Mademoiselle", "Madame", "Monsieur")
                    .setRequired(true),
                new BranchPage(this, "Voulez-vous utiliser Yana sur Internet ?")
                    .addBranch("Oui",
                    		new IPAdress_ExtInfoPage(this, "Remplissez les champs")
			                    .setRequired(true)
		            )
		            .addBranch("Non",
		            		new IPAdressInfoPage(this, "Remplissez les champs")
			                    .setRequired(true)
		            )
		            .setRequired(true),
		        new MultipleFixedChoicePage(this, "Vous voulez activer ...")
                	.setChoices("Le ShakeService", "Les événements", "La synthèse vocale", "La mise à jour des commandes au démarrage")
       );
    }
}
