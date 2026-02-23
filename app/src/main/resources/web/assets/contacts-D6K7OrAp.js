import{N as g,r as C,ao as $,ap as h,aq as p}from"./index-DUFKAMTy.js";const l=C(new Map);let d=!1;function i(n){return n.replace(/[\s\-\(\)\+]/g,"").slice(-10)}function z(n){let a="";p(n.firstName)||p(n.lastName)?a=`${n.lastName}${n.middleName}${n.firstName}`:a=[n.firstName,n.middleName,n.lastName].filter(e=>e).join(" ");const t=n.suffix?`, ${n.suffix}`:"",o=`${n.prefix} ${a} ${t}`.trim();return o||(n.emails.length?n.emails[0].value:"")}const v=h`
  query allContacts {
    contacts(offset: 0, limit: 10000, query: "") {
      ...ContactFragment
    }
  }
  ${$}
`;function y(){const{fetch:n}=g({handle:(e,r)=>{if(!r&&e){const s=new Map;for(const u of e.contacts){const c=z(u);if(c)for(const f of u.phoneNumbers){const m=i(f.value);if(m&&s.set(m,c),f.normalizedNumber){const N=i(f.normalizedNumber);N&&s.set(N,c)}}}l.value=s,d=!0}},document:v,variables:()=>({})});function a(){d||n()}function t(e){if(!e)return"";const r=i(e);return l.value.get(r)||""}function o(e){return t(e)||e||"-"}return{loadContacts:a,getContactName:t,getDisplayName:o,contactsMap:l}}export{y as u};
