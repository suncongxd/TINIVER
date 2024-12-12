# TINIVER

- Timed Noninterferences Verifier.

------------------------

## Install Dependencies

- JDK 17
```
sudo apt-get install openjdk-17-jdk openjdk-17-jre
java -version
```

- IntelliJ IDEA
```
sudo snap install intellij-idea-community --classic
```

- Uppaal
  - Download `Uppaal 64-bit Windows and Linux, version 4.1.26-1`. (https://uppaal.org/downloads/)
  - Unzip Uppaal into the root directory of TINIVER.

------------------------

## Structure of TINIVER

- NoninterferenceVerify (Implementation of TINIVER)
- inputModel (Models under verification, categorized by their original paper)
- outputModel (The intermediate models generated from the input model and delivered to Uppaal for verification)

------------------------

## Build TINIVER

- Import the project `NoninterferenceVerify` into IDEA.
- Build Artifacts -> Rebuild (Generate `NoninterferenceVerify.jar` into `TINIVER` directory).

------------------------

## Usage

```
usage: NoninterferenceVerify
 -h         help
 -i <arg>   input_model_path
 -o <arg>   output_dir_for_intermediate_model
 -p <arg>   verified_security_property
 -u <arg>   uppaal_path
```
 
- Example
 
```
 java -jar NoninterferenceVerify.jar -i inputModel/cav18/BNNI_2.xml -o outputModel/cav18 -p BNNI -u uppaal64-4.1.26-1/bin-Linux/server
```
------------------------

## Contributor

- Qiaosen Liu - Xidian University
- Cong Sun - Xidian University
- Yunbo Wang - Xidian University


