{
  pkgs ? import <nixpkgs> { },
}:
pkgs.mkShellNoCC {
  preferLocalBuild = true;
  allowSubstitutes = false;

  name = "town-of-babel-backend";

  packages = with pkgs; [
    jdk21
    gradle
    jdt-language-server
    python3.pkgs.lizard
  ];
}
