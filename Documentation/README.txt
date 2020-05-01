Overview of this project and its documentation

##################
## Introduction ##
##################

Welcome to ToneSet, a project for the University of Saskatchewan HCI lab. I (Eric Redekopp) inherited this project from
another HCI employee (Alex Scott) in the summer of 2019 at which time it was a few hundred lines of JavaFX code which
basically just generated background noise and performed a basic ramp test. This version is also implemented in Java
but runs on Android and contains a much greater variety of tests as well as tools for storing, viewing, and processing
data on those tests. Almost none of Alex's original code remains in this project but I've left his name on classes that
were partially written by him.

ToneSet exists to play "tones" through an Android device and allow users to respond when they hear a tone (and not
respond when they do not hear a tone). The end goal of this study is to be able to predict with some level of accuracy
whether a user will be able to hear a tone in a given background noise environment. This program learns about the
properties of the user's hearing and the background noise environment through a test or a suite of tests known as a
"calibration test", which tests the user's ability to hear single pitches (usually but not necessarily sine waves) at
various volumes and see how many times each was heard. The predictive power of the "calibration test" is checked using a
"confidence test" which selects various pitches and volumes of tones which it predicts should range in audibility from
0% to 100% and several values in between, based on the results of the calibration. It then plays each pitch several
times in randomized order to find an estimate for the "true" audibility of each, and saves some data on the difference
between its prediction and the "true" probability found by the confidence test. The tones tested by the confidence test
need not be similar to the ones in the calibration test - in fact, the ultimate result of the project is hoped to be a
system which predicts the audibility of any arbitrary tone with reasonable accuracy given just a quick "ramp test". See
hearing-tests.txt for more information on hearing tests.


##################################
## Current State of the Project ##
##################################

Maybe come back and read this after you're done reading the rest of the docs; this will reference stuff that I don't
explain until later.

I was not able to fully complete this application within my NSERC grant time and some things remain unfinished. I added
some new functionality later on in the semester and tested it a little bit, but to be quite honest, my testing has not
been as thorough as it probably could have been and you may run into a couple of bugs. I am writing this document on the
last day before my new NSERC term starts, so I literally am not allowed to keep testing this app even if I wanted to
(and I don't).

I've left a couple of TODOs in the source code; these are just things that I noticed while writing these docs but didn't
have time to really go through and look at, but you may want to look at them.


Capabilities:

    The project can do a full 3-phase calibration with sine waves, and can do confidence tests with sine waves in single
    pitches, intervals, or melodies, and with piano tones in single pitches only. Wav tests with real ringtones are not
    implemented at this time, but they were at one point and I'm quite sure it's just that they need to be tossed in to
    the new and improved architecture. Other tests are shown as available on the UI but will throw a
    TestNotAvailableException if you try to start one.

    The application can compare the results of a confidence test to the results of a participant's calibration data and
    give some information on the accuracy of the model's estimates.

Known Bugs / incomplete sections / things to think about:

    - You will probably want to add more tests, depending on what exactly you want the final version to look like.

    - The audio sometimes sounds weird and crackly. I do not understand why this is, and it seems to be
      device-dependent. My best guess is that this is because the application opens and closes the audio line after each
      tone; it might be a smarter idea to just write null data to the line when a tone isn't playing instead. It could
      also be because of the interaction between the BackgroundNoiseController and the Model's audio functions.

    - I'm still not entirely convinced that the internal mixer doesn't significantly affect the ratio of volumes. For
      example, it seems like tones get a little louder during the 1/2 second of silence in the crowd noise recording. Is
      it pushing down loud volumes on one track to better hear the quieter sounds of a different track? Is this
      something we need to be thinking about? Or does it not matter as long as it's consistent?

    - Good Lord this test is boring. Probably the main reason it hasn't been extremely thoroughly tested is that after
      testing yourself for the 3rd or 4th time you just want to stab yourself in the eardrums with a sharpened pencil.
      If you really want to make this a cool study you should game-ify it or something.

    - HearingTestController.ConfidenceTest has a field for the number of trials but I think it still just uses a default
      value and ignores what the user asked it to do.

    - There are a whole lot of classes for hearing tests that are just stubs, or are implemented but unused. This is
      because I focused way too much on features and not enough on usability in the beginning, and couldn't fit all the
      features into my improved, more usable version in time.



#######################
## Table of Contents ##
#######################

This directory contains documentation on the project and the code therein. There are like 11,000 lines of code in here
and I don't have an infinite budget for writing docs, so this information is far from complete. My email is at the
bottom of this document; I will happily reply to questions about this code, provided that I can still remember how it
works.

I strongly recommend that you read hearing-tests.txt and mvc.txt first because they provide the highest-level
descriptions of what this application does and how it all works together

    README.txt (this file):
        Overview information and this table of contents

    data.txt
        Information about how test data is stored at runtime (not to be confused with file-io.txt)

    file-io.txt:
        Information pertaining to the saving and loading of Participant data from files on disk

    future.txt
        Tips on how to get started on some tasks that you'll possibly want to do in the future.

    hearing-tests.txt
        Descriptions of the different types of hearing tests that this application may perform, as well as explanations
        of how they work, how they are started by the system, and how to add new ones.

    interaction.txt
        Descriptions of all the different Views in this application and how the user is meant to interact with them

    noise.txt
        Explanation of how background noise is generated

    mvc.txt
        Information about the "Model-View-Controller" structure of this project, and how the major components of this
        application interact with each other.

    tones.txt
        Descriptions of the different tones playable by this application and how they are implemented


#######################
## Project Structure ##
#######################

This project builds using Gradle. I know very little about Gradle, so don't touch the default AndroidStudio settings
unless you know what you're doing.

Most of the files in this project are organized into modules, which are explained here:

    Files not in a module:
        Activities and Views for the Android framework. Not a lot of real work happens in these files except for
        MainActivity and InitActivity which handle the GUI on the login and main pages.

        Participant is a storage class to hold all of a single participant's test results

        UtilFunctions contains various static generic functions that are used in several different files

    Control:
        Models and controllers for different aspects of the program (views are handled by the non-modularized View and
        Activity classes described above). These classes are described in detail in mvc.txt

    HearingTest:
        Everything to do with the actual hearing tests to be performed by this application.

        Container:
            Classes for storing the results of one or more tests

        Test:
            Classes representing the different types of tests that can be performed by this application.
            See hearing-tests.txt for more information

        Tone:
            Classes representing the different tones that can be played by this application. See tones.txt for more
            information.


#############################
## Personal note from Eric ##
#############################

This project was the first time I've ever been paid to program, it was pretty much entirely designed and implemented
by myself, at the time of this writing it is the biggest project I've ever worked on, and was mostly completed just
after my 2nd year of university with minimal supervision. It was a sort of trial-by-fire for me and it's taught me
more than any class ever has, but it means that I was learning almost everything as I went (I didn't even know what a
thread was when I started this project and I had almost no experience implementing GUIs) and so things are a little
messy in places. This project has changed forms many times, includes some little bits of features that I thought about
implementing but later decided not to, there are some  elements in the structure that don't really make sense with the
way things are currently laid out but made sense back when they were first written, and there are some elements of the
structure that didn't even really make sense when I first wrote them.

In hindsight, there's a lot of things I would have done differently (most notably, just thinking things through better
before starting to write code), and some of the code in here is definitely not my best work. I've done a little bit of
work to clean things up, and I've documented everything to the best of my ability. The documents in this folder should
give you a good start on extending the current functionality, but certainly don't describe every single aspect of this
code and how it works. Do not hesitate to contact me at err291@mail.usask.ca if you have any questions about this code,
or if you'd just like to get mad at me for leaving you a mess (sorry).