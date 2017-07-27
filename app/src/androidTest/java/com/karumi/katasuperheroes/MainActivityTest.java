/*
 * Copyright (C) 2015 Karumi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.karumi.katasuperheroes;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.matcher.IntentMatchers;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.karumi.katasuperheroes.di.MainComponent;
import com.karumi.katasuperheroes.di.MainModule;
import com.karumi.katasuperheroes.model.SuperHero;
import com.karumi.katasuperheroes.model.SuperHeroesRepository;
import com.karumi.katasuperheroes.recyclerview.RecyclerViewInteraction;
import com.karumi.katasuperheroes.ui.view.MainActivity;
import com.karumi.katasuperheroes.ui.view.SuperHeroDetailActivity;

import it.cosenonjaviste.daggermock.DaggerMockRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.mockito.AdditionalMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class) @LargeTest public class MainActivityTest {

  @Rule public DaggerMockRule<MainComponent> daggerRule =
      new DaggerMockRule<>(MainComponent.class, new MainModule()).set(
          new DaggerMockRule.ComponentSetter<MainComponent>() {
            @Override public void setComponent(MainComponent component) {
              SuperHeroesApplication app =
                  (SuperHeroesApplication) InstrumentationRegistry.getInstrumentation()
                      .getTargetContext()
                      .getApplicationContext();
              app.setComponent(component);
            }
          });

  @Rule public IntentsTestRule<MainActivity> activityRule =
      new IntentsTestRule<>(MainActivity.class, true, false);

  @Mock SuperHeroesRepository repository;

  @Test public void showsEmptyCaseIfThereAreNoSuperHeroes() {
    givenThereAreNoSuperHeroes();

    startActivity();

    onView(withText("¯\\_(ツ)_/¯")).check(matches(isDisplayed()));
  }

  @Test public void showEmptyCaseIfThereIsNothing() {
    givenThereIsAnything();

    startActivity();

    onView(withText("¯\\_(ツ)_/¯")).check(matches(not(isDisplayed())));
  }

  @Test public void showNoSpinnerWheelWhenThereIsAnySuperHero() {
    givenThereIsAnySuperHero(true);

    startActivity();

    onView(withId(R.id.progress_bar)).check(matches(not(isDisplayed())));
  }

  @Test public void showFilledCaseIfThereIsAnySuperHero() {
    givenThereIsOneSuperHero();

    startActivity();

    onView(withId(R.id.iv_avengers_badge)).check(matches(isDisplayed()));
  }

  @Test public void shouldListHaveAllTheSuperHeroNames() {
    List<SuperHero> superHeroList = givenThereIsAnySuperHero(true);

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
      .withItems(superHeroList)
      .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
        @Override
        public void check(SuperHero item, View view, NoMatchingViewException e) {
          matches(hasDescendant(withText(item.getName()))).check(view, e);
        }
      });
  }

  @Test public void shouldItemHaveAvengerBadge() {
    List<SuperHero> superHeroList = givenThereIsAnySuperHero(true);

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
      .withItems(superHeroList)
      .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
        @Override
        public void check(SuperHero item, View view, NoMatchingViewException e) {
            matches(hasDescendant(allOf(withId(R.id.iv_avengers_badge), isDisplayed())))
                    .check(view, e);
        }
      });
  }

  @Test public void shouldItemNotHaveAvengerBadge() {
    List<SuperHero> superHeroList = givenThereIsAnySuperHero(false);

    startActivity();

    RecyclerViewInteraction.<SuperHero>onRecyclerView(withId(R.id.recycler_view))
      .withItems(superHeroList)
      .check(new RecyclerViewInteraction.ItemViewAssertion<SuperHero>() {
        @Override
        public void check(SuperHero item, View view, NoMatchingViewException e) {
          matches(hasDescendant(allOf(withId(R.id.iv_avengers_badge), not(isDisplayed()))))
                  .check(view, e);
        }
      });
  }

  @Test public void shouldNavigateToSuperHeroDetailActivity() {
    List<SuperHero> superHeroList = givenThereIsAnySuperHero(true);

    startActivity();

    onView(withId(R.id.recycler_view))
      .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));

    SuperHero selectedHero = superHeroList.get(0);

    intended(hasComponent(SuperHeroDetailActivity.class.getCanonicalName()));
    intended(hasExtra("super_hero_name_key", selectedHero.getName()));
  }

  private void givenThereAreNoSuperHeroes() {
    when(repository.getAll()).thenReturn(Collections.<SuperHero>emptyList());
  }


  private void givenThereIsAnything() {
    List<SuperHero> superHeroList = new ArrayList<>();
    superHeroList.add(new SuperHero("ss", "ss", false, "ss"));
    when(repository.getAll()).thenReturn(superHeroList);
  }

  private void givenThereIsOneSuperHero() {
    List<SuperHero> superHeroList = new ArrayList<>();

    superHeroList.add(
            new SuperHero("Bocasecaman", "https://i.ytimg.com/vi/TKynoTS1UsQ/hqdefault.jpg",
                          true,
                          "Tiene la lengua como un gatete!"));

    when(repository.getAll()).thenReturn(superHeroList);
  }

  private List<SuperHero> givenThereIsAnySuperHero(boolean isAvenger) {
    List<SuperHero> superHeroList = new ArrayList<>();

    for (int i = 0; i < 50; i++) {
      superHeroList.add(
        new SuperHero("Bocasecaman" + i, "https://i.ytimg.com/vi/TKynoTS1UsQ/hqdefault.jpg",
                      isAvenger ,
                      "Tiene la lengua como un gatete!" + i));
      
    }

    when(repository.getAll()).thenReturn(superHeroList);

    return superHeroList;
  }

  private MainActivity startActivity() {
    return activityRule.launchActivity(null);
  }
}