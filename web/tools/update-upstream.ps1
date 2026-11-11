param([string]$Tag)
$ErrorActionPreference='Stop'
$repoRoot=(Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
function GitCommand{
 param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments)
 $result=& git -C $repoRoot @Arguments
 if($LASTEXITCODE){throw "Git failed: $($Arguments -join ' ')"};return $result
}
if(-not $Tag){$Tag=(Invoke-RestMethod 'https://api.github.com/repos/00-Evan/shattered-pixel-dungeon/releases/latest').tag_name}
if($Tag -notmatch '^v\d+\.\d+\.\d+$'){throw 'Use an official stable release tag.'}
if((GitCommand remote get-url upstream) -ne 'https://github.com/00-Evan/shattered-pixel-dungeon.git'){throw 'Unexpected upstream remote.'}
if(GitCommand status --porcelain){throw 'Commit or preserve current changes before updating.'}
$old=Get-Content (Join-Path $repoRoot 'web/upstream.json') -Raw|ConvertFrom-Json
if($Tag -eq $old.tag){Write-Output "Already based on $Tag.";exit 0}
if([version]$Tag.TrimStart('v') -le [version]$old.tag.TrimStart('v')){throw 'Update target must be newer.'}
$branch='update/'+$Tag+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss');GitCommand switch -c $branch
GitCommand -c http.version=HTTP/1.1 fetch --filter=blob:none --depth=512 upstream "refs/tags/${Tag}:refs/tags/${Tag}"
$commit=GitCommand rev-parse ($Tag+'^{}')
$cache=Join-Path $repoRoot ('.web-update/'+$Tag+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $cache|Out-Null
$archive=Join-Path $cache 'official.tar.gz'
Invoke-WebRequest "https://codeload.github.com/00-Evan/shattered-pixel-dungeon/tar.gz/refs/tags/$Tag" -OutFile $archive
$source=Join-Path $cache 'source';New-Item -ItemType Directory -Path $source|Out-Null
& tar -xzf $archive --strip-components=1 -C $source;if($LASTEXITCODE){throw 'Archive extraction failed.'}
Get-ChildItem $source -Recurse -Force -File|Select-Object -ExpandProperty FullName|& git -C $repoRoot hash-object -w --stdin-paths|Out-Null
if($LASTEXITCODE){throw 'Source hydration failed.'}
GitCommand -c user.name='Portal maintainer' -c user.email='portal@localhost' merge --no-edit $Tag
$json=@{repository=$old.repository;tag=$Tag;upstreamCommit=$commit}|ConvertTo-Json
[IO.File]::WriteAllText((Join-Path $repoRoot 'web/upstream.json'),$json,[Text.UTF8Encoding]::new($false))
& (Join-Path $PSScriptRoot 'build.ps1')
GitCommand add web/upstream.json
GitCommand -c user.name='Portal maintainer' -c user.email='portal@localhost' commit -m "Validate web adapter against official $Tag"
Write-Output "Validated candidate: $branch. Browser-test old saves and floor transitions before deploying web/build/public."
