#!/bin/zsh
export GITHUB_WORKSPACE=./
mkdir -p $GITHUB_WORKSPACE/assembled/libs/

./gradlew build -x test
export CONDUCTOR_ENTERPRISE_VERSION=$(./gradlew properties --no-daemon --console=plain -q | grep "^appVersion:" | awk '{printf $2}')
echo $CONDUCTOR_ENTERPRISE_VERSION
cp $GITHUB_WORKSPACE/workers/build/libs/workes*.jar $GITHUB_WORKSPACE/assembled/libs/workers.jar

export tag="local$(date +%s)"
export tag="0.0.6"
echo $GH_TOKEN | docker login ghcr.io -u orkesio --password-stdin

docker build --platform linux/amd64 -f workers/Dockerfile . --tag ghcr.io/orkes-io/orkes-conductor-load-tester/orkes-conductor-load-tester:$tag
docker push ghcr.io/orkes-io/orkes-conductor-load-tester/orkes-conductor-load-tester:$tag


