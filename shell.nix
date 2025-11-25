{
  pkgs ? import <nixpkgs> { },
}:
pkgs.mkShellNoCC {
  preferLocalBuild = true;
  allowSubstitutes = false;

  name = "town-of-babel";

  packages = with pkgs; [
    docker
    docker-buildx
    docker-compose
  ];
}
