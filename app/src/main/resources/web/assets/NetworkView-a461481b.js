import{_ as x}from"./MonacoEditor.vuevuetypescriptsetuptruelang-9e6c8162.js";import{_ as y}from"./EditToolbar.vuevuetypescriptsetuptruelang-6f5b5087.js";import{_ as N}from"./Breadcrumb-1bb571cf.js";import{d as $,u as b,r as m,h as C,x as r,ac as p,by as c,i as h,c as B,a as F,p as i,k as S,m as d,aV as g,o as U}from"./index-6ca6ab55.js";const q={class:"page-container"},D={class:"main"},T=$({__name:"NetworkView",setup(A){const{t:s}=b(),o=m(0),n=m(""),a=m("");C({handle:(l,e)=>{e?r(s(e),"error"):(n.value=l.networkConfig.netplan,a.value=l.networkConfig.netmix)},document:p`
    query {
      networkConfig {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});const{mutate:_,loading:f,onDone:v}=h({document:p`
    mutation applyNetplanAndNetmix($netplan: String!, $netmix: String!) {
      applyNetplan(config: $netplan) {
        __typename
      }
      applyNetmix(config: $netmix) {
        ...NetworkConfigFragment
      }
    }
    ${c}
  `});v(()=>{r(s("saved"))});function V(){!n.value||!a.value||_({netplan:n.value,netmix:a.value})}return(l,e)=>{const k=N,w=y,u=x;return U(),B("div",q,[F("div",D,[i(k,{current:()=>l.$t("page_title.network")},null,8,["current"]),i(w,{modelValue:o.value,"onUpdate:modelValue":e[0]||(e[0]=t=>o.value=t),save:V,loading:S(f),tabs:["/etc/netplan/config.yaml","/etc/plainbox/netmix.yaml"]},null,8,["modelValue","loading","tabs"]),d(i(u,{language:"yaml",height:"700",modelValue:n.value,"onUpdate:modelValue":e[1]||(e[1]=t=>n.value=t)},null,8,["modelValue"]),[[g,o.value===0]]),d(i(u,{language:"yaml",height:"700",modelValue:a.value,"onUpdate:modelValue":e[2]||(e[2]=t=>a.value=t)},null,8,["modelValue"]),[[g,o.value===1]])])])}}});export{T as default};
