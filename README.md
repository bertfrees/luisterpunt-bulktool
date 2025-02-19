# [DAISY Pipeline][pipeline] based conversion tool for Luisterpuntbibliotheek

## Usage

    jre/bin/java [JAVA_OPTIONS] -jar main.jar FORMAT FILE DIRECTORY [--PARAMETER VALUE [--PARAMETER VALUE ...]]

`FORMAT` is the output format. It must be one of the following:

- `dtbook`: DAISY XML
- `brf`: braille file for embossing
- `ebraille`: HTML with Unicode braille text (for electronic braille reading)

`FILE` is the input file. The supported input file formats (determined by the file extension) are:

- `.odt` (OpenDocument Text)
- `.xml` (DAISY XML) (only for the `brf` and `ebraille` output formats)

The following parameters can be specified:

- `dots`: `6` (default) or `8`: produce 6- or 8-dot braille (only for the `brf` and `ebraille`
  output formats)
- `duplex`: `true` (default) or `false`: emboss on both sides of the paper or not (only for the
  `brf` output format)
- `capital-letters`: `true` (default) or `false`: mark capital letters in braille or not (only for the `brf`
  and `ebraille` output formats)

`JAVA_OPTIONS` are optional arguments passed to the Java runtime environment. The following options
are available:

- `-Dorg.daisy.pipeline.logdir=DIRECTORY`: specify the location for log files

## Build prerequisites

- Java Development Kit >= 8
- GNU Make >= 3.82
- Docker

## Build instructions

    git submodule update --init --recursive
    make

The command above compiles the code, creates an executable for Windows, runs tests on it, and
packages it in a ZIP file. The file can be found in the "dist" directory.

## License

Copyright 2024, 2025 [Luisterpuntbibliotheek](https://www.luisterpuntbibliotheek.be/)

This program is free software: you can redistribute it and/or modify
it under the terms of the [GNU General Public License][gpl] as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.


[pipeline]: https://daisy.org/activities/software/pipeline/
[gpl]: https://www.gnu.org/licenses/gpl-3.0.txt
