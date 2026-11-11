// Toggle controls around the existing game frame; never replace its document.
export function createManagementView({document,location,history}){
  document.documentElement.classList.toggle('management',new URL(location.href).searchParams.get('manage')==='1');
  function set(enabled){
    const url=new URL(location.href);
    if(enabled)url.searchParams.set('manage','1');else url.searchParams.delete('manage');
    history.replaceState(null,'',url.pathname+url.search+url.hash);
    document.documentElement.classList.toggle('management',enabled);
  }
  return {open:()=>set(true),close:()=>set(false)};
}
